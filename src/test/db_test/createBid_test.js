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

export let options = {
    scenarios: {
        // 입찰 시나리오: 마감 시간 임박하여 서서히 유저가 몰리는 상황 모사
        bidding_scenario: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },  // [워밍업] 10초 동안 0명 -> 30명으로 서서히 증가 (JVM, DB 커넥션 풀 적응)
                { duration: '10s', target: 200 }, // [피크 도달] 다음 10초 동안 30명 -> 200명으로 급증
                { duration: '20s', target: 200 }, // [피크 유지] 20초 동안 200명의 트래픽 폭격 유지 (진짜 성능 측정 구간)
                { duration: '10s', target: 0 },   // [쿨다운] 10초 동안 서서히 0명으로 종료
            ],
            exec: 'bidding',
        },

        // 조회 시나리오: 구경꾼들이 꾸준히 유입되며 입찰자와 함께 피크를 찍는 상황 모사
        viewing_scenario: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },  // [워밍업] 10초 동안 0명 -> 50명으로 서서히 증가
                { duration: '10s', target: 200 }, // [피크 도달] 다음 10초 동안 50명 -> 200명으로 급증
                { duration: '20s', target: 200 }, // [피크 유지] 20초 동안 200명의 조회 트래픽 유지
                { duration: '10s', target: 0 },   // [쿨다운] 10초 동안 서서히 0명으로 종료
            ],
            exec: 'viewing',
        },
    },
    // 옵션: 전체 테스트가 끝날 때 p95 지표를 명확히 보기 위한 임계값
    thresholds: {
        http_req_duration: ['p(95)<500'], // 전체 요청의 95%가 500ms 이내
        http_req_failed: ['rate<0.01'],   // 에러율 1% 미만 방어
    }
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