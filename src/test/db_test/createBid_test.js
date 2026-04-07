import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend, Counter } from 'k6/metrics';

// [기존] 응답 시간 추적
const bidDuration = new Trend('bid_duration');
const viewDuration = new Trend('view_duration');

// 👈 [추가] 상태별 카운팅을 위한 Counter 지표 생성
const bidSuccessCount_200 = new Counter('bid_success_count_200');       // 200: 입찰 성공
const bidAmountFail = new Counter('bid_amount_fail_count_400');    // 400: 입찰 금액 에러
const bidLockFailCount = new Counter('bid_lock_fail_count_429');    // 429: 분산락 대기 시간 초과 (Fast-fail)
const bidErrorCount = new Counter('bid_error_500');           // 5xx: DB 커넥션 타임아웃, 데드락 등 서버 에러

export const options = {
    scenarios: {
        bidding_scenario: {
            executor: 'constant-vus',
            vus: 100,
            duration: '30s',
            exec: 'bidding',
        },
        viewing_scenario: {
            executor: 'constant-vus',
            vus: 200,
            duration: '30s',
            exec: 'viewing',
        },
    },
};

export function bidding() {
    const currentIter = exec.scenario.iterationInTest;
    const bidAmount = 110000 + (currentIter * 1000);
    const userId = (exec.vu.idInTest % 100) + 10;
    const hotspotAuctionId = Math.floor(Math.random() * 3) + 2;

    const payload = JSON.stringify({
        auctionId: hotspotAuctionId,
        bidAmount: bidAmount,
        isAutoBid: false,
        maxAutoBidAmount: null
    });

    const params = { headers: { 'Content-Type': 'application/json', 'X-User-Id': userId } };

    const res = http.post(`http://localhost:8085/api/loadtest/bids`, payload, params);

    bidDuration.add(res.timings.duration);

    // 👈 [수정] 상태 코드별로 정확하게 분류해서 카운팅 및 로깅
    if (res.status === 200) {
        bidSuccessCount_200.add(1);
    } else if (res.status === 400) {
        bidAmountFail.add(1); // 분산락이 DB 부하를 막아낸 훈장 같은 지표!
    } else if (res.status === 429){
        bidLockFailCount.add(1);
    }
    else {
        bidErrorCount.add(1);
        console.log(`[입찰 치명적 에러] status=${res.status} body=${res.body}`);
    }

    check(res, {
        '[입찰] is status 200': (r) => r.status === 200,
    });

    sleep(0.1);
}

export function viewing() {
    const targetAuctionId = Math.floor(Math.random() * 10) + 2;
    const res = http.get(`http://localhost:8085/api/public/products/${targetAuctionId}`);

    viewDuration.add(res.timings.duration);

    check(res, {
        '[조회] is status 200': (r) => r.status === 200,
        '[조회] time OK (500ms 이하)': (r) => r.timings.duration < 500,
    });

    sleep(0.5);
}