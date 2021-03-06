-- 덧글 테이블 구성
CREATE TABLE reply(
rep_no NUMBER(38) PRIMARY KEY,   -- 덧글 번호(seq)
rep_cont VARCHAR(4000) NOT NULL, -- 덧글 내용
rep_date DATE,					 -- 덧글 작성일
rep_ref NUMBER(38),				 -- 덧글&답글 묶을번호(seq)
rep_step NUMBER(38),			 -- n번째 답글
rep_level NUMBER(38),			 -- 덧글&답글 순서 정렬
bo_no NUMBER(38),				 -- 게시글번호 참조 컬럼
mem_no NUMBER(38)				 -- 회원번호 참조 컬럼
);

DROP SEQUENCE rep_no_seq;
DROP TABLE reply

CREATE SEQUENCE rep_no_seq
START WITH 0
INCREMENT BY 1
MINVALUE 0
NOCACHE;

SELECT *
FROM reply r
INNER JOIN member m
ON r.mem_no=m.mem_no
WHERE r.bo_no=1
ORDER BY r.rep_ref ASC,r.rep_level

SELECT * FROM reply;
SELECT rep_no_seq.nextval FROM DUAL;

-- 참조 컬럼 생성
ALTER TABLE reply
ADD CONSTRAINT rep_bo_no_fk FOREIGN KEY(bo_no)
REFERENCES board(bo_no)

ALTER TABLE reply
ADD CONSTRAINT rep_mem_no_fk FOREIGN KEY(mem_no)
REFERENCES member(mem_no)



