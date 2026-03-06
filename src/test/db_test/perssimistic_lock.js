import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8085";
const URL = `${BASE_URL}/api/loadtest/bids`;

// 예: "2,3,4,5,6"
const AUCTION_IDS = (__ENV.AUCTION_IDS || "2,3,4,5,6")
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n));

const START_HIGHEST = Number(__ENV.START_HIGHEST || 100000);
const SUCCESS_RATE = Number(__ENV.SUCCESS_RATE || 0.8);

const MARGIN = Number(__ENV.MARGIN || 200000000);       // 핫 여러개면 보수적으로
const SLOPE_PER_MS = Number(__ENV.SLOPE_PER_MS || 50000);
const JITTER_MAX = Number(__ENV.JITTER_MAX || 200000);

const TEST_START_MS = Date.now();

export const options = {
    scenarios: {
        burst_2s_1000: {
            executor: "constant-arrival-rate",
            rate: 200,
            timeUnit: "1s",
            duration: "30s",
            preAllocatedVUs: 100,
            maxVUs: 500,
        },
    },
};

function mustFinite(name, v) {
    if (!Number.isFinite(v)) throw new Error(`${name} not finite: ${v}`);
    return v;
}

function randInt(min, max) {
    mustFinite("randInt.min", min);
    mustFinite("randInt.max", max);
    if (min > max) throw new Error(`randInt range invalid: ${min} > ${max}`);
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function userId() {
    const vu = exec.vu.idInTest;
    const iter = exec.vu.iterationInScenario;
    return ((vu * 13 + iter) % 1000) + 1;
}

function pickAuctionId() {
    // "각 경매에 고르게" 가려면 iteration 기반으로 라운드로빈이 깔끔함
    const i = exec.scenario.iterationInTest; // 전체 iteration 카운터 (지원 안되면 아래 대체 사용)
    if (Number.isFinite(i)) return AUCTION_IDS[i % AUCTION_IDS.length];

    // 대체: vu/iter 조합
    const vu = exec.vu.idInTest;
    const iter = exec.vu.iterationInScenario;
    return AUCTION_IDS[(vu + iter) % AUCTION_IDS.length];
}

function successAmount() {
    const elapsedMs = Date.now() - TEST_START_MS;
    mustFinite("elapsedMs", elapsedMs);

    const vu = exec.vu.idInTest;
    const iter = exec.vu.iterationInScenario;
    const tie = ((vu % 100) * 10 + (iter % 10));

    const amount =
        START_HIGHEST +
        MARGIN +
        elapsedMs * SLOPE_PER_MS +
        tie * 1000 +
        randInt(0, JITTER_MAX);

    mustFinite("amount", amount);
    return Math.floor(amount / 1000) * 1000;
}

function failAmount() {
    // 실패는 그냥 낮게(고정)
    return 1000;
}

function bidAmount() {
    return Math.random() < SUCCESS_RATE ? successAmount() : failAmount();
}

export default function () {
    const payload = {
        auctionId: pickAuctionId(),
        bidAmount: bidAmount(),
        isAutoBid: false,
        maxAutoBidAmount: null,
    };

    const res = http.post(URL, JSON.stringify(payload), {
        headers: {
            "Content-Type": "application/json",
            "X-User-Id": String(userId()),
        },
        timeout: "10s",
    });

    check(res, {
        "2xx": (r) => r.status === 200 || r.status === 201,
        "expected 4xx": (r) => [400, 409].includes(r.status),
        "no 5xx": (r) => r.status < 500,
    });
}
