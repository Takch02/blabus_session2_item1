import http from 'k6/http';
import { check, sleep } from 'k6';

// =============================
// 🔥 테스트 옵션 (30초)
// =============================
export const options = {
    scenarios: {
        test_scenario: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },  // ramp-up
                { duration: '10s', target: 50 },  // steady
                { duration: '10s', target: 100 }, // spike
            ],
            exec: 'mixedScenario',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'], // SLA
    },
};

// =============================
// 🔧 설정값 (여기만 바꾸면 됨)
// =============================
const BASE_URL = 'http://localhost:8085/api/public/auctions';

// 테스트 모드 (여기 바꿔가면서 테스트)
const TEST_MODE = 'mixed';
// 'baseline' | 'deep' | 'mixed'

// =============================
// 🔧 유틸
// =============================
function random(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// =============================
// 🎯 시나리오
// =============================
export function mixedScenario() {
    let page;

    if (TEST_MODE === 'baseline') {
        // 🔹 기존 테스트 (문제 숨겨짐)
        page = 0;

    } else if (TEST_MODE === 'deep') {
        // 🔹 병목 터뜨리는 테스트
        page = random(1000, 5000);

    } else {
        // 🔹 실제 서비스 패턴 (추천)
        const rand = Math.random();

        if (rand < 0.7) {
            page = 0; // 첫 페이지
        } else if (rand < 0.9) {
            page = random(10, 100); // 중간
        } else {
            page = random(1000, 5000); // 깊은 페이지
        }
    }

    const url = `http://3.39.236.179:8085/api/public/auctions/?isPremium=false&status=IN_PROGRESS&page=${page}&size=16`;

    const res = http.get(url);

    check(res, {
        'status 200': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        console.log(`❌ status=${res.status}, body=${res.body}`);
    }

    sleep(0.3); // 과도한 DDOS 방지 + 현실성
}