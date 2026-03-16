import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Trend } from 'k6/metrics'; // 👈 [추가] Trend 모듈 임포트

// 👈 [추가] 입찰과 조회의 응답 시간을 따로 기록할 커스텀 지표 생성
const bidDuration = new Trend('bid_duration');
const viewDuration = new Trend('view_duration');

export const options = {
    scenarios: {
        // [시나리오 1: 포식자] 150명이 3개의 경매에 미친듯이 입찰 (DB 커넥션 점유 시도)
        // [시나리오 2: 피해자] 50명이 10초 동안 끊임없이 일반적인 경매 상세 정보 조회 (Read-Only)
        viewing_scenario: {
            executor: 'constant-vus',
            vus: 50,
            duration: '10s',
            exec: 'viewing', // 아래의 viewing() 함수 실행
        },
    },
};

export function bidding() {
    const currentIter = exec.scenario.iterationInTest;
    const bidAmount = 110000 + (currentIter * 1000);
    const userId = (exec.vu.idInTest % 100) + 10; // 안전한 유저 ID 풀
    const hotspotAuctionId = Math.floor(Math.random() * 3) + 2; // 2, 3, 4번 경매 타겟

    const payload = JSON.stringify({
        auctionId: hotspotAuctionId,
        bidAmount: bidAmount,
        isAutoBid: false,
        maxAutoBidAmount: null
    });

    const params = { headers: { 'Content-Type': 'application/json', 'X-User-Id': userId } };

    const res = http.post(`http://localhost:8085/api/loadtest/bids`, payload, params);

    bidDuration.add(res.timings.duration);

    check(res, {
        '[입찰] is status 200 or 400 or 429': (r) => r.status === 200 || r.status === 400 || r.status === 429,
    });
    if (res.status !== 200 && res.status !== 400 && res.status !== 429) {
        console.log(`\n🚨 [입찰 실패 범인 검거] 
        - 상태 코드: ${res.status}
        - 유저 ID: ${userId}
        - 응답 바디: ${res.body}`);
    }
    sleep(0.1);
}

export function viewing() {
    const targetAuctionId = Math.floor(Math.random() * 10) + 2;

    // 단순 GET 요청 (DB에서 읽기만 수행)
    const res = http.get(`http://localhost:8085/api/public/products/${targetAuctionId}`);

    viewDuration.add(res.timings.duration);

    check(res, {
        '[조회] is status 200': (r) => r.status === 200,
        '[조회] time OK (200ms 이하)': (r) => r.timings.duration < 200, // 단순 조회는 빨라야 정상!
    });


    sleep(0.5); // 일반 유저의 클릭 텀 모사
}