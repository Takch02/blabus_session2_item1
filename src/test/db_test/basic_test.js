import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 설정 (여기가 부하를 결정해)
export const options = {
    // 가상 사용자(VUser) 10명
    vus: 10,
    // 60초 동안 지속
    duration: '30s',

    // (선택사항) 기준점 설정: "95%의 요청이 5초 안에 안 끝나면 테스트 실패로 간주해라"
    thresholds: {
        http_req_duration: ['p(95)<5000'],
    },
};

export default function () {
    // page = 0, size = 10
    const url = 'http://localhost:8085/api/public/auctions/?status=IN_PROGRESS&page=0&size=10&sort=newest%2CDesc';

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 3. 요청 보내기
    const res = http.get(url, params);

    // 4. 결과 체크 (성공했는지 확인)
    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 5. 대기 (너무 인정사정없이 쏘면 DDOS니까 1초 텀을 줌)
    sleep(1);
}