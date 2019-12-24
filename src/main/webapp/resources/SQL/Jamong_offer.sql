-- 제안하기 테이블 구성
CREATE TABLE offer(
off_no NUMBER(38) PRIMARY KEY,      -- 제안 번호
off_id VARCHAR2(50) NOT NULL,           -- 작가 ID
off_email VARCHAR2(50) NOT NULL,    -- 제안자 이메일
off_domain VARCHAR2(50) NOT NULL,   -- 제안자 이메일 도메인
off_phone1 VARCHAR2(10) NOT NULL,              -- 제안자 전화번호
off_phone2 VARCHAR2(10) NOT NULL,
off_phone3 VARCHAR2(10) NOT NULL,
off_date DATE,                      -- 제안 날짜
off_item VARCHAR2(50) NOT NULL,     -- 제안 종류
off_cont VARCHAR2(4000) NOT NULL,   -- 제안 내용
off_file1 VARCHAR2(200),            -- 첨부파일 1
off_file2 VARCHAR2(200),
off_file3 VARCHAR2(200),
mem_no NUMBER(38)                   -- 제안자 회원번호
);

DROP TABLE offer;

-- 시퀀스 생성
CREATE SEQUENCE off_no_seq
START WITH 0
INCREMENT BY 1
MINVALUE 0
NOCACHE;

DROP SEQUENCE off_no_seq;

-- 참조키 설정
ALTER TABLE offer
ADD CONSTRAINT off_mem_no_fk FOREIGN KEY (mem_no)
REFERENCES member(mem_no);

ALTER TABLE offer
ADD CONSTRAINT off_off_id_fk FOREIGN KEY (off_id)
REFERENCES member(mem_no);

SELECT * FROM offer ORDER BY off_no DESC;

SELECT * FROM member ORDER BY mem_no DESC;

UPDATE member set mem_author=1 where mem_id='jamong01';
commit;