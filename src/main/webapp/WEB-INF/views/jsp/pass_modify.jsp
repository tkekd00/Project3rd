<%@ page contentType="text/html; charset=UTF-8"%>
<jsp:include page="../include/header.jsp" />
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" type="text/css" href="./resources/css/pass_modify.css" />
<link rel="stylesheet" type="text/css" href="./resources/css/font.css"/>
<script src="./resources/js/jquery.js"></script>
<script src="./resources/js/pass_modify.js"></script>
<title>비밀번호수정페이지입니다</title>
</head>
<body>
	<div id ="pass_modify_main_wrap">
		<div id="pass_modify_main_div">
				<!-----------------로고------------------------>
				<div>
					<a href="./"><img id="pass_modify_img_logo"
						src="./resources/img/logo2.png" /></a>
				</div>
				
				<label id="pass_modify_label">비밀번호 확인</label>
				
				<!-------------------이름 텍필 이메일 택필---------------------->
				
			<div id="pass_modify_sub_div">
					
						<div class="pass_modify_color">
                        <input id="pass_modify_id" name="pass_modify_id" class="pass_modify_text" readonly="readonly" value="${m.mem_id }"></div>
						<div class="pass_modify_error" id="pass_modify_error_pass"></div>						
						
						<div class="pass_modify_color">
                        <input id="pass_modify_pass" name="pass_modify_pass" class="pass_modify_text" placeholder="비밀번호" type="password"></div>
						<div class="pass_modify_error" id="pass_modify_error_pass_check"></div>						

                <input type="submit" id="pass_modify_lastbtn" value="비밀번호 확인">
			</div>
		</div>
	</div>
</body>
</html>
<%@ include file="../include/footer.jsp"%>