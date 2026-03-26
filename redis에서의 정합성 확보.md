# 문제 1

**redis에서의 정합성 확보**

---

`Redis` `Spring Boot` 

**Background**

---

- 유저가 24시간 내에 먹은 음식 횟수를 순위 매기는 기능 추가
- redis에 (음식이름, 먹은 횟수)와 (음식이름_n, 현재시간) 두 ZSet 생성
- 유저가 버튼 클릭 시 (24시간 지난 기록 삭제) → ([음식,시간] data 추가) → (음식,먹은 횟수 +1) 구조로 진행

**Problem**

---

```java
public RankEntryDto increment(String name) {
    removeExpiredEvents();         //시간 만료시 삭제 메서드 (redis명령 1)
    long now = Instant.now(clock).getEpochSecond();  // 현재 시간
    
    // redis 명령 2,3
    redisTemplate.opsForZSet().add(RANK_EVENT_KEY, buildEventMember(name), now);
    Double score = redisTemplate.opsForZSet().incrementScore(RANK_KEY, name, 1);
    
    long count = score == null ? 1L : score.longValue();
    return new RankEntryDto(name, count);
}
```

*Problem 1.* 한 메서드에서 만료이벤트 삭제 및 랭킹 반영/ (음식,시간) 추가 / (음식,횟수) 증가  3번의 Redis 명령이 실행 됨

→ 네트워크 왕복 비용 증가

→ 여러 유저가 동시에 increment() 요청 시 만료 이벤트 랭킹에 중복 반영과 같은 race 문제가 생길 수 있음<br>

**Sol 1.** 해당 명령들을 Lua 스크립트로 묶어 1회 호출 되도록 함 

```java
    public RankEntryDto increment(String name) {
        long now = Instant.now(clock).getEpochSecond();
        long cutoff = now - retentionSeconds;
        Number score = redisTemplate.execute(    
                incrementScript,     //Redis 스크립트
                List.of(RANK_KEY, RANK_EVENT_KEY),
                name,
                buildEventMember(name),
                Long.toString(now),
                Long.toString(cutoff)
        );

        return new RankEntryDto(name, toLong(score, "increment"));
    }
```

*Problem 2.*  redis는 rollback 기능이 없기에 여전히 정합성 보장이 안됨


**Sol 2.** 문제가 생길 시 관리자가 복구 할 수 있는 복구 로직 추가

→ 랭킹 시스템에 정합성 로직을 추가하는 것은 과하다고 판단

→ Lua 스크립트에서 명령 순서를 (삭제) → (음식,시간) 추가 → (음식,횟수) 추가 하도록 구현

- 삭제만 실행 후 비정상 종료 시 : data 1건 누락 (critical하지 않다고 봄)
- (음식,시간) 추가 이후 비정상 종료 시 : 누적 될 경우 관리자가 수동으로 (음식,시간) ZSet을 통해 새 (음식,횟수) ZSet생성

*Problem 3.* 개별 유저 요청이 만료 정리 비용까지 떠 안아 특정 유저의 속도가 느려질 수 있음

→ 만료 데이터를 ZRANGEBYSCORE로 받아와서 이를 for문으로 돌리며 삭제 및 랭킹 업데이트하는 방식

→ 만료된 이벤트 개수(M) + 그 이벤트 안에서 음식 종류 수 (N) 만큼의 시간복잡도 발생 O(M+N) 


**Sol 3-1.** 정리 전용 스케줄러 도입 (X)

→ 유저 요청시 (삭제) 하는 방식이 아닌 백그라운드에서 특정 주기 마다 요청하는 방식으로 변경

→ 만료된 이벤트가 랭킹 업데이트에 반영될 수 있음

**Sol 3-2.** 버킷 구조 도입 (트래픽 증가 시 고려)

- 기존 방식                                                                                           버킷 방식

<img width="1206" height="573" alt="image" src="https://github.com/user-attachments/assets/6e5ff337-4c5d-41f9-b8fa-8a449071161a" />


- 버킷 구조 도입 시 증가 요청 성능이 개선되지만 조회 성능이 떨어짐

|  | 현재 구조 | 버킷 구조 |  |
| --- | --- | --- | --- |
| 랭킹 증가 반영 | O(M+N) | O(1) | B: 버킷 개수 |
| 랭킹 조회 | O(1) | O(B+F) | F: 버킷 내부 음식 종류 |

→ 랭킹 증가: 비동기 처리 시 집계 누락이 생길 수 있음

→ 랭킹 조회: 비동기 후속 작업으로 분리해도 별 문제 없음

→ 트래픽 증가 시 랭킹 증가 요청 시간을 줄이는 게 더 효율적
