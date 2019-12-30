package com.jamong.controller;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.jamong.domain.BoardVO;
import com.jamong.domain.CategoryVO;
import com.jamong.domain.MemberVO;
import com.jamong.domain.ReplyVO;
import com.jamong.service.BoardService;
import com.jamong.service.BookService;
import com.jamong.service.CategoryService;
import com.jamong.service.MemberService;
import com.jamong.service.ReplyService;
import com.oreilly.servlet.MultipartRequest;

import pwdconv.PwdChange;
import timeChanger.TIME_MAXIMUM;

@Controller
public class BoardController {

	@Autowired
	private BoardService boardService;
	@Autowired
	private MemberService memberService;
	@Autowired
	private CategoryService catService;
	@Autowired
	private ReplyService repService;
	@Autowired
	private BookService bookService;

	@RequestMapping("@{mem_id}/{bo_no}")
	public String user_readCont(@PathVariable String mem_id, @PathVariable int bo_no, BoardVO bo, Model model,
			HttpServletResponse response, HttpServletRequest request, HttpSession session) throws Exception {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		session=request.getSession();
		
		MemberVO readM = (MemberVO)session.getAttribute("m");

		HashMap<String,Object> bm = new HashMap<>();
		bm.put("mem_id",mem_id);
		bm.put("bo_no",bo_no);

		BoardVO nextVo = this.boardService.getNextBoardCont(bm);
		BoardVO preVo = this.boardService.getPreBoardCont(bm);
		bo = this.boardService.getUserBoardCont(bo_no);

		/** lock 0 비공개, 1 공개, 2 정지, 3 삭제 **/
		if(bo.getBo_lock() == 0) { // 비공개 글일때 ( lock가 0일때 )
			if(readM == null) {
					out.println("<script>");
					out.println("alert('비공개 처리된 글 입니다.');");
					out.println("history.back();");
					out.println("</script>");
					return null;
			}else {
				if(readM.getMem_no()!=bo.getMem_no()) {
					out.println("<script>");
					out.println("alert('비공개 처리된 글 입니다.');");
					out.println("history.back();");
					out.println("</script>");
					return null;
				}
			}			
		}else if(bo.getBo_lock() == 3) { // 삭제된 게시글일때 ( lock가 3일떄 )
			out.println("<script>");
			out.println("alert('삭제된 게시글 입니다.');");
			out.println("history.back();");
			out.println("</script>");
			return null;
		}
		
		List<ReplyVO> repList = this.repService.getUserBoardContReply(bo_no);
		int replyCount = this.repService.getUserReplyCount(bo_no);
		List<BoardVO> catList = this.boardService.getUserBoardCatArticle(bo.getCat_name());
		List<BoardVO> bList = this.boardService.getUserBoardContList(bo.getMem_no());

		// 날짜 출력타입 변경
		SimpleDateFormat org_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat title_format = new SimpleDateFormat("MMM d, yyyy",new Locale("en","US"));		
		Date format_date = org_format.parse(bo.getBo_date());
		String title_date = title_format.format(format_date);

		for(int i=0;i<bList.size();i++) {
			Date bListFormat_date = org_format.parse(bList.get(i).getBo_date());
			String bListTitle_date = title_format.format(bListFormat_date);
			bList.get(i).setBo_date(bListTitle_date);
		}
		bo.setBo_date(title_date);

		// 시간 계산 해서 방금, 몇분전 띄우기
		for(int i=0;i<repList.size();i++) {
			Date repListFormat_date = org_format.parse(repList.get(i).getRep_date());
			String repList_date = TIME_MAXIMUM.formatTimeString(repListFormat_date);
			repList.get(i).setRep_date(repList_date);
		}
		// html -> text
		for(int i=0;i<bList.size();i++) {
			String htmlText = bList.get(i).getBo_cont();
			String normalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			String oneSpace = normalText.replaceAll("&nbsp; "," ");
			if(normalText.length()>100) {
				String hundredText = oneSpace.substring(0,100);					
				bList.get(i).setBo_cont(hundredText);
			}else {
				bList.get(i).setBo_cont(oneSpace);
			}
		}
		for(int i=0;i<catList.size();i++) {
			String htmlText = catList.get(i).getBo_cont();
			String normalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			String oneSpace = normalText.replaceAll("&nbsp; "," ");
			if(normalText.length()>100) {
				String hundredText = oneSpace.substring(0,100);								
				catList.get(i).setBo_cont(hundredText);
			}else {
				catList.get(i).setBo_cont(oneSpace);
			}
		}

		model.addAttribute("next",nextVo);
		model.addAttribute("pre",preVo);
		model.addAttribute("replyCount",replyCount);
		model.addAttribute("rList",repList);
		model.addAttribute("catList",catList);
		model.addAttribute("bList",bList);
		model.addAttribute("bo", bo);
		model.addAttribute("mem_id", mem_id);

		return "jsp/read";
	}// 게시글 읽기 페이지

	@PostMapping("artdel/{bo_no}")
	@ResponseBody
	public int user_delArticle(@PathVariable int bo_no,
			HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		int flag = 0;

		MemberVO artM = (MemberVO)session.getAttribute("m");		

		if(artM != null) {
			this.boardService.articleDelete(bo_no);

			flag = 1;
		}else {

			flag = 2;
		}

		return flag;
	}// 게시글 삭제

	@RequestMapping("@{mem_id}/{bo_no}/write")
	public ModelAndView user_editWrite(@PathVariable String mem_id, @PathVariable int bo_no, BoardVO bo,
			HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception{
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		ModelAndView mv = new ModelAndView();

		MemberVO m = (MemberVO)session.getAttribute("m");

		if (m != null) {
			bo = this.boardService.getUserBoardCont(bo_no);
			mv.addObject("bo",bo);
			mv.setViewName("jsp/jamong_edit");
			return mv;
		}else {
			out.println("<script>");
			out.println("alert('로그인이 필요한 페이지입니다!');");
			out.println("location='/jamong.com/login/1';");
			out.println("</script>");
		}
		return null;
	} // 수정 폼 이동

	@RequestMapping("write")
	public ModelAndView user_write(HttpServletResponse response, HttpServletRequest request, HttpSession session)
			throws Exception {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		session = request.getSession();
		ModelAndView mv = new ModelAndView();

		MemberVO m = (MemberVO) session.getAttribute("m");

		if (m == null) {
			out.println("<script>");
			out.println("alert('로그인이 필요한 페이지입니다!');");
			out.println("location='login/1';");
			out.println("</script>");
		} else {
			mv.setViewName("jsp/jamong_write");
			return mv;
		} // if else => 로그인 전 / 후

		return null;
	}// 글쓰기 페이지 이동

	@RequestMapping("write_ok")
	public String user_write_ok(BoardVO b, HttpServletResponse response, HttpServletRequest request,
			HttpSession session) throws Exception {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		session = request.getSession();

		String saveFolder = request.getRealPath("/resources/upload");
		int fileSize = 100 * 1024 * 1024; // 첨부파일 최대크기
		MultipartRequest multi = null;
		multi = new MultipartRequest(request, saveFolder, fileSize, "UTF-8");

		UUID uuid = UUID.randomUUID(); // 랜덤한 이름값 생성

		// MultipartRequest로부터 각 파라미터값 저장
		String bo_title = multi.getParameter("bo_title");
		String bo_subtitle = multi.getParameter("bo_subtitle");
		String bo_cont = multi.getParameter("bo_cont");
		String bo_color = multi.getParameter("bo_color");
		int bo_lock = Integer.parseInt(multi.getParameter("bo_lock"));
		int bo_type = Integer.parseInt(multi.getParameter("bo_type"));
		int bo_titlespace=Integer.parseInt(multi.getParameter("bo_titlespace"));
		String cat_name = multi.getParameter("cat_name");

		MemberVO m = (MemberVO) session.getAttribute("m");
		int mem_no = m.getMem_no();

		File UpFile1 = multi.getFile("bo_thumbnail");
		if (UpFile1 != null) {
			String fileName = UpFile1.getName();
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH) + 1;
			int date = c.get(Calendar.DATE);

			String homedir = saveFolder + "\\" + "thumbnail" + "\\" + year + "-" + month + "-" + date;
			String DBdir = "/jamong.com/resources/upload/thumbnail/" + year + "-" + month + "-" + date;
			File path1 = new File(homedir);
			if (!(path1.exists())) {
				path1.mkdirs(); // 폴더 생성
			} // if => 해당 폴더가 없을때

			Random r = new Random();
			int index = fileName.lastIndexOf(".");
			String fileExtendsion = fileName.substring(index + 1);

			String refileName = uuid.toString() + year + month + date;
			// 업로드파일명 + 년월일 + 난수 + 확장자
			String encryptionName = PwdChange.getPassWordToXEMD5String(refileName);
			String fileDBName = DBdir + "/" + encryptionName + "." + fileExtendsion;

			UpFile1.renameTo(new File(homedir + "/" + encryptionName + "." + fileExtendsion));

			b.setBo_thumbnail(fileDBName);
		} // if => 파일이 있을 때

		System.out.println(bo_color);

		b.setBo_color(bo_color);
		b.setBo_title(bo_title);
		b.setBo_subtitle(bo_subtitle);
		b.setBo_cont(bo_cont);
		b.setBo_titlespace(bo_titlespace);
		b.setBo_lock(bo_lock);
		b.setBo_type(bo_type);
		b.setCat_name(cat_name);
		b.setMem_no(mem_no);

		HashMap<String,Object> bm = new HashMap<>();
		bm.put("b",b);
		bm.put("mem_no",mem_no);
		bm.put("mem_id",m.getMem_id());

		this.boardService.insertBoard(bm);

		out.println("<script>");
		out.println("alert('게시글이 등록되었습니다!')");
		out.println("location='/jamong.com/';");
		out.println("</script>");

		return null;
	}// user_write_ok() => 유저 글 등록

	@RequestMapping("@{mem_id}/{bo_no}/write_ok")
	public String user_edit_ok(@PathVariable int bo_no, BoardVO b, HttpServletResponse response, HttpServletRequest request,
			HttpSession session) throws Exception {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		session = request.getSession();		
		HashMap<String,Object> bm = new HashMap<>();		

		String saveFolder = request.getRealPath("/resources/upload");
		int fileSize = 100 * 1024 * 1024; // 첨부파일 최대크기
		MultipartRequest multi = null;
		multi = new MultipartRequest(request, saveFolder, fileSize, "UTF-8");

		UUID uuid = UUID.randomUUID(); // 랜덤한 이름값 생성

		// MultipartRequest로부터 각 파라미터값 저장
		String bo_title = multi.getParameter("bo_title");
		String bo_subtitle = multi.getParameter("bo_subtitle");
		String bo_cont = multi.getParameter("bo_cont");
		String bo_color = multi.getParameter("bo_color");
		int bo_lock = Integer.parseInt(multi.getParameter("bo_lock"));
		int bo_type = Integer.parseInt(multi.getParameter("bo_type"));
		int bo_titlespace=Integer.parseInt(multi.getParameter("bo_titlespace"));
		String cat_name = multi.getParameter("cat_name");
		
		int flag = Integer.parseInt(multi.getParameter("thumb_remove"));

		MemberVO m = (MemberVO) session.getAttribute("m");

		File UpFile1 = multi.getFile("bo_thumbnail");
		if (UpFile1 != null) {
			String fileName = UpFile1.getName();
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH) + 1;
			int date = c.get(Calendar.DATE);

			String homedir = saveFolder + "\\" + "thumbnail" + "\\" + year + "-" + month + "-" + date;
			String DBdir = "/jamong.com/resources/upload/thumbnail/" + year + "-" + month + "-" + date;
			File path1 = new File(homedir);
			if (!(path1.exists())) {
				path1.mkdirs(); // 폴더 생성
			} // if => 해당 폴더가 없을때

			Random r = new Random();
			int index = fileName.lastIndexOf(".");
			String fileExtendsion = fileName.substring(index + 1);

			String refileName = uuid.toString() + year + month + date;
			// 업로드파일명 + 년월일 + 난수 + 확장자
			String encryptionName = PwdChange.getPassWordToXEMD5String(refileName);
			String fileDBName = DBdir + "/" + encryptionName + "." + fileExtendsion;

			UpFile1.renameTo(new File(homedir + "/" + encryptionName + "." + fileExtendsion));

			b.setBo_thumbnail(fileDBName);
		} // if => 파일이 있을 때

		if(flag == 1) {
			b.setBo_color("1");
		}else if(flag == 0) {
			b.setBo_color(bo_color);
		}else if(bo_color == null) {
			b.setBo_color("1");
		}

		b.setBo_title(bo_title);
		b.setBo_subtitle(bo_subtitle);
		b.setBo_cont(bo_cont);
		b.setBo_titlespace(bo_titlespace);
		b.setBo_lock(bo_lock);
		b.setBo_type(bo_type);
		b.setCat_name(cat_name);

		bm.put("b",b);
		bm.put("bo_no",bo_no);

		if(m != null) {
			this.boardService.updateBoard(bm);
			out.println("<script>");
			out.println("alert('게시글이 수정되었습니다!');");
			out.println("location='/jamong.com/';");
			out.println("</script>");
		} else {
			out.println("<script>");
			out.println("alert('세션이 만료되었습니다 로그인페이지로 돌아갑니다.');");
			out.println("location='/jamong.com/login/1';");
			out.println("</script>");
		}
		return null;
	} // 게시글 수정 컨트롤러

	@PostMapping("imageUpload")
	@ResponseBody
	public void ImageUpload(HttpServletResponse response, HttpServletRequest request) throws Exception {

		PrintWriter out = response.getWriter();
		String saveFolder = request.getRealPath("/resources/upload");
		int fileSize = 10 * 1024 * 1024;

		MultipartRequest multi = new MultipartRequest(request, saveFolder, fileSize, "UTF-8");

		UUID uuid = UUID.randomUUID();

		File UpFile = multi.getFile("file");
		if (UpFile != null) {
			String fileName = UpFile.getName();
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH) + 1;
			int date = c.get(Calendar.DATE);

			String homedir = saveFolder + "\\" + "user" + "\\" + year + "-" + month + "-" + date;

			File path = new File(homedir);
			if (!(path.exists())) {
				path.mkdirs();
			} // if => 폴더 경로 생성

			int index = fileName.lastIndexOf(".");
			String fileExtendsion = fileName.substring(index + 1);

			String refileName = uuid.toString() + year + month + date;
			String encryptionName = PwdChange.getPassWordToXEMD5String(refileName);
			String fileUpName = "/jamong.com/resources/upload/user/" + year + "-" + month + "-" + date + "/"
					+ encryptionName + "." + fileExtendsion;

			UpFile.renameTo(new File(homedir + "/" + encryptionName + "." + fileExtendsion));

			out.println(fileUpName);
		}
	}// imageUp() => 썸머노트 이미지 업로드 이름변경

	@RequestMapping("new_posts")
	public ModelAndView user_new_posts(BoardVO b) throws Exception{
		List<BoardVO> bList = this.boardService.getListAll(b);
		SimpleDateFormat org_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (int i = 0; i < bList.size(); i++) {
			String htmlTitle = bList.get(i).getBo_title();
			String normarTitle = htmlTitle.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			String titleSpace = normarTitle.replaceAll("&nbsp;"," ");
			bList.get(i).setBo_title(titleSpace);

			String htmlText = bList.get(i).getBo_cont();
			String normalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			String oneSpace = normalText.replaceAll("&nbsp; "," ");
			bList.get(i).setBo_cont(oneSpace);

			Date org_date = org_format.parse(bList.get(i).getBo_date());
			String changeDate = TIME_MAXIMUM.formatTimeString(org_date);
			bList.get(i).setBo_date(changeDate);
		}
		ModelAndView mv = new ModelAndView();
		mv.addObject("bList", bList);
		mv.setViewName("jsp/new_posts");
		return mv;
	}

	@PostMapping("sympathy_up/{bo_no}")
	@ResponseBody
	public int sympathy_up(@PathVariable int bo_no, 
			HttpServletRequest request,
			HttpSession session) {
		session = request.getSession();

		MemberVO m = (MemberVO) session.getAttribute("m");

		int result =-1;
		if(m!=null) {
			BoardVO bo = new BoardVO();
			bo.setBo_no(bo_no); bo.setMem_no(m.getMem_no());
			result = this.boardService.sympathyUp(bo);
		}
		return result;
	}

	@PostMapping("sympathy_down/{bo_no}")
	@ResponseBody
	public int sympathy_down(@PathVariable int bo_no, 
			HttpServletRequest request,
			HttpSession session) {
		session = request.getSession();
		MemberVO m = (MemberVO) session.getAttribute("m");

		int result =-1;
		if(m!=null) {
			BoardVO bo = new BoardVO();
			bo.setBo_no(bo_no); bo.setMem_no(m.getMem_no());
			result = this.boardService.sympathyDown(bo);
		}
		return result;
	}

	@PostMapping("boardLock/{bo_no}/{bo_lock}")
	@ResponseBody
	public int boardLock(@PathVariable int bo_no,@PathVariable int bo_lock,
			HttpServletRequest request,
			HttpSession session) {
		session = request.getSession();
		MemberVO m = (MemberVO) session.getAttribute("m");
		int result =-1;
		if(m!=null) {
			BoardVO bo = new BoardVO();
			bo.setBo_no(bo_no); bo.setMem_no(m.getMem_no());
			bo.setBo_lock(bo_lock);
			result = this.boardService.switchBoardLock(bo);
		}
		return result;
	}

	@RequestMapping("best_load")
	public ResponseEntity<List<BoardVO>> best_load(){
		ResponseEntity<List<BoardVO>> entity = null;

		try {
			entity = new ResponseEntity<>(this.boardService.bestList(),HttpStatus.OK);		
		}catch(Exception e) {
			e.printStackTrace();
			entity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return entity;
	}

	@PostMapping("infinitiScrollDown")
	@ResponseBody
	public List<BoardVO> infinitiScrollDownPOST(String bo_no) throws Exception{
		int bo_noToStart = Integer.parseInt(bo_no) - 1;
		List<BoardVO> data = this.boardService.infinitiScrollDown(bo_noToStart);
		SimpleDateFormat org_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (int i = 0; i < data.size(); i++) {
			String htmlTitle = data.get(i).getBo_title();
			String normarTitle = htmlTitle.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			String titleSpace = normarTitle.replaceAll("&nbsp;"," ");
			data.get(i).setBo_title(titleSpace);

			String htmlText = data.get(i).getBo_cont();
			String normalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			data.get(i).setBo_cont(normalText);

			Date repListFormat_date = org_format.parse(data.get(i).getBo_date());
			String repList_date = TIME_MAXIMUM.formatTimeString(repListFormat_date);
			data.get(i).setBo_date(repList_date);
		}
		return data;
	}

	@RequestMapping("search")
	public ModelAndView user_search(HttpServletResponse response, HttpServletRequest request)throws Exception {
		ModelAndView mv = new ModelAndView();
		//url 주소의 parameter값 가져오기
		String w = request.getParameter("w");		// where 검색 목차(글-post,책-book,작가-author)
		String s = request.getParameter("s");		// step 하위 분기(글[accuracy,recent],카테고리)
		String text = request.getParameter("q");	// query 검색어 (띄어쓰기 단위)
		String q = text.replaceAll(" ", "\\|");
		//+표시로 parameter값을 가져오면 띄어쓰기로 표현되는데, sql문으로써 이용하기 위해서 파이프라인(|)으로 replace해줌 

		HashMap<String,Object> searchMap = new HashMap<>();
		searchMap.put("s", s);
		searchMap.put("q", q);
		SimpleDateFormat org_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat title_format = new SimpleDateFormat("MMM d, yyyy",new Locale("en","US"));		

		if(w.equals("post")) {				//글검색
			List<BoardVO> boardList = this.boardService.getSearchPost(searchMap);			

			for (int i = 0; i < boardList.size(); i++) {
				// 시간 계산 해서 방금, 몇분전 띄우기
				Date bListFormat_date = org_format.parse(boardList.get(i).getBo_date());
				String bListTitle_date = title_format.format(bListFormat_date);
				boardList.get(i).setBo_date(bListTitle_date);

				//미리보여주는 글 태그 없앰
				String htmlText = boardList.get(i).getBo_cont();
				String normalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
				String oneSpace = normalText.replaceAll("&nbsp;"," ");
				if(oneSpace.length()>200) {
					String hundredText = oneSpace.substring(0,200);								//100글자만 홈페이지에 노출되도록 변경
					boardList.get(i).setBo_cont(hundredText);
				}else {
					boardList.get(i).setBo_cont(oneSpace);
				}
			}

			mv.addObject("boardList", boardList);
		}else if(w.equals("book")) {		//책검색
			List<BoardVO> bookList = this.bookService.getSearchBook(searchMap);
			for(int i=0;i<bookList.size();i++) {
				String bookHtmlText = bookList.get(i).getBookVO().getBook_name();
				String bookStrippedText = bookHtmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
				String bookOneSpace = bookStrippedText.replaceAll("&nbsp;","");					
				bookList.get(i).getBookVO().setBook_name(bookOneSpace);
			}
			mv.addObject("bookList",bookList);
		}else if(w.equals("author")) {		//작가검색
			List<CategoryVO> catList = this.catService.listCategory();
			List<MemberVO> memberList = this.memberService.getSearchMember(searchMap);
			mv.addObject("catList",catList);
			mv.addObject("memberList",memberList);
		}

		mv.addObject("w",w);
		mv.addObject("s",s);
		mv.addObject("q",text);
		mv.setViewName("jsp/search_result");

		return mv;
	}

	@PostMapping("profile_scroll")
	@ResponseBody
	public List<BoardVO> profileScrolling(
			String bo_no, String mem_no,
			HttpServletResponse response, 
			HttpServletRequest request
			) throws Exception{
		System.out.println(bo_no);
		System.out.println(mem_no);
		int data_bo = Integer.parseInt(bo_no);
		int data_mno = Integer.parseInt(mem_no);
		
		SimpleDateFormat b_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat date_format = new SimpleDateFormat("MMM d, yyyy",new Locale("en","US"));	
		HashMap<Object,Object> scroll = new HashMap<>();
		scroll.put("bo_no", data_bo);
		scroll.put("mem_no", data_mno);
		List<BoardVO> pfData = this.boardService.profileScroll(scroll);
		for(int i=0; i<scroll.size(); i++) {

			Date mpListFormat_date = b_format.parse(pfData.get(i).getBo_date());
			String mpListTitle_date = TIME_MAXIMUM.formatTimeString(mpListFormat_date);
			pfData.get(i).setBo_date(mpListTitle_date);
			
			// 미리보여주는 글 태그 없앰 (제목)
			String titleText = pfData.get(i).getBo_title();
			String titleNomarText = titleText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			pfData.get(i).setBo_title(titleNomarText);

			//미리보여주는 글 태그 없앰 (내용)
			String htmlText = pfData.get(i).getBo_cont();
			String nomalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
			String oneSpace = nomalText.replaceAll("&nbsp; "," ");
			pfData.get(i).setBo_cont(oneSpace);
			System.out.println(pfData.get(i).getBo_no());
		}
		return pfData;
	}
	
	@PostMapping("search_scroll")
	@ResponseBody
	public Object search_scroll(
			String num, String w, String s, String text,
			HttpServletResponse response, HttpServletRequest request
			)throws Exception {
		int n = Integer.parseInt(num);				// number 검색시에 사용될 rowNum(10이후 숫자들...)
		String q = text.replaceAll("\\+", "\\|");
		HashMap<String,Object> searchMap = new HashMap<>();
		searchMap.put("n", n);
		searchMap.put("s", s);
		searchMap.put("q", q);
		SimpleDateFormat org_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat title_format = new SimpleDateFormat("MMM d, yyyy",new Locale("en","US"));		

		if(w.equals("post")) {				//글검색
			List<BoardVO> boardList = this.boardService.getSearchScrollPost(searchMap);			

			for (int i = 0; i < boardList.size(); i++) {
				// 시간 계산 해서 방금, 몇분전 띄우기
				Date bListFormat_date = org_format.parse(boardList.get(i).getBo_date());
				String bListTitle_date = title_format.format(bListFormat_date);
				boardList.get(i).setBo_date(bListTitle_date);

				//미리보여주는 글 태그 없앰
				String htmlText = boardList.get(i).getBo_cont();
				String normalText = htmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
				String oneSpace = normalText.replaceAll("&nbsp;"," ");
				if(oneSpace.length()>200) {
					String hundredText = oneSpace.substring(0,200);								//100글자만 홈페이지에 노출되도록 변경
					boardList.get(i).setBo_cont(hundredText);
				}else {
					boardList.get(i).setBo_cont(oneSpace);
				}
			}

			return boardList;
		}else if(w.equals("book")) {		//책검색
			List<BoardVO> bookList = this.bookService.getSearchScrollBook(searchMap);
			for(int i=0;i<bookList.size();i++) {
				String bookHtmlText = bookList.get(i).getBookVO().getBook_name();
				String bookStrippedText = bookHtmlText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
				String bookOneSpace = bookStrippedText.replaceAll("&nbsp;","");					
				bookList.get(i).getBookVO().setBook_name(bookOneSpace);
			}
			return bookList;
		}else if(w.equals("author")) {		//작가검색
			List<MemberVO> memberList = this.memberService.getSearchScrollMember(searchMap);
			return memberList;
		}
		return null;
	} 
	
}
