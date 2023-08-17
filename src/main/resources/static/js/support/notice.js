$(function() {
	
	const params = new URLSearchParams(location.search);
	const defaultKeyword = params.get('keyword');
	if (defaultKeyword) {
		$("input[name=keyword]").val(defaultKeyword);
		getNoticeList();
	}
	
    // 탭 클릭 이벤트
    $('li.tab-link').click(function() {
        // 모든 탭과 버튼의 활성화 클래스 제거
        $('li.tab-link').removeClass('current');
        $('button.btn').removeClass('current');

        // 현재 탭과 버튼에 활성화 클래스 추가
        $(this).addClass('current');
        $(this).find('button.btn').addClass('current');
    });
	
	// 지역조회
	$("#theater").prop("disabled", true);
	
	let $selectLocation = $("#location").empty();
	$selectLocation.append(`<option value="" selected disabled>지역선택</option>`)
	
	$.getJSON("/support/lost/getLocation", function(locations) {
		locations.forEach(function(loc) {
			let option = `<option value="${loc.no}"> ${loc.name}</option>`;
			$selectLocation.append(option);
		})
		
	})
	
	// 극장조회
	$("#location").change(function() {
		$("#theater").prop("disabled", false);
		
		let locationNo = $(this).val();
		let $selectTheater = $("#theater").empty();
		
		$selectTheater.append(`<option value="" selected disabled>극장선택</option>`)
		
		$.getJSON("/support/lost/getTheaterByLocationNo?locationNo="+ locationNo, function(theaters){
			theaters.forEach(function(thr) {
				let option = `<option value="${thr.no}"> ${thr.name}</option>`;
				$selectTheater.append(option);
			})
		})
		
	});
	
	// 검색버튼 클릭했을 때
	$("#searchBtn").click(function() {
		$("input[name=page]").val(1);
		
		getNoticeList();
	});
	
	// 폼 전송 이벤트
	$("#actionForm").on('submit', function(e) {
		e.preventDefault();
		getNoticeList();
	});

	
	// 페이저번호클릭했 떄
	$('.pagination').on('click', '.page-number-link', function(event) {
		event.preventDefault();
		let page = $(this).attr("data-page");
		
		// 모든 페이지 번호 링크에서 active 클래스 제거
        $('.page-number-link').removeClass('active');

        // 클릭한 페이지 번호에만 active 클래스 추가
        $(this).addClass('active'); 
		
		$("input[name=page]").val(page);
		
		getNoticeList();
	})	
	  
	// 탭을 클릭했을 때
    $("li.tab-link").click(function() {
	     let categoryNo = $(this).find('button').attr("data-category-no");
			$("input[name=catNo]").val(categoryNo);
			$("input[name=page]").val(1);
			
			getNoticeList();
    });
    
    function getNoticeList() {
		
		let categoryNo = $("input[name=catNo]").val();
		let page = $("input[name=page]").val();
		let locationNo = $("select[name=locationNo]").val();
		let theaterNo = $("select[name=theaterNo]").val();
		let keyword = $("input[name=keyword]").val();
		
		let $tbody = $(".noticeList").empty()
		let $pagination = $(".pagination").empty();
		
		$.getJSON("/support/notice/list", {catNo:categoryNo, locationNo:locationNo, theaterNo:theaterNo,page:page,  keyword:keyword}, function(result) {
			
			// 총 건수 업데이트
       		$('#totalCnt').text(result.pagination.totalRows);
       		
       		let noticeList = result.noticeList;
       		let pagination = result.pagination;
       		
       		if (noticeList.length === 0) {
				   $tbody.append(`
				   		<tr><th colspan='5' style="text-align:center;">조회된 내역이 없습니다.</th></tr>
				   `);
			} else {
				 
				 const tbodyHtml = noticeList.map(function(notice, index) {
					return `
				<tr>
	                <td>${notice.no}</td>
	                <td>${notice.theater.name == null ? 'MGV' : notice.theater.name}</td>
	                <td>${notice.type == '공지' ? '공지' : '이벤트'}</td>
	                <td style="text-align:left;">
				            	<a class="text-black text-decoration-none"
				            		href="/support/notice/detail?no=${notice.no}"
				            		data-no="${notice.no}">
				            		${notice.title }
				            	</a>
				            </td>
	                <td>${notice.updateDate}</td>
	            </tr>
				`	 
				}).join("\n");
				
				$tbody.html(tbodyHtml);
				
				// 페이지네이션
				let firstContent = `
		            <li class="page-item">
		                <a title="첫번째 페이지 보기"
		                   href="list?page=1"
		                   class="page-link page-number-link control first"
		                   data-page="1">1</a>
		            </li>
		        `;
				$pagination.append(firstContent);

				if (result.pagination.currentBlock > 1) {
					let prePage = (pagination.currentBlock -1)* 10
					let nextContent = `
		                <li class="page-item">
		                    <a title="이전 10페이지 보기"
		                       href="list?page=${prePage}"
		                       class="page-link page-number-link control prev"
		                       data-page="${prePage}">${prePage}</a>
		                </li>
		            `;
					$pagination.append(nextContent);
				}


				for (let i = pagination.beginPage; i <= pagination.endPage; i++) {
					let content = `
					 <li class="page-item">
                    	<a href="list?page=${i}" 
                      	 	class="page-link page-number-link ${i == pagination.page ? 'active' : ''}"
                       		data-page="${i}">${i}</a>
               		 </li>
               		
				`;
					$pagination.append(content);
				}


				if (result.pagination.currentBlock < result.pagination.totalBlocks) {
					let nextpage = (pagination.currentBlock)* 10 + 1
					let nextContent = `
		                <li class="page-item">
		                    <a title="이후 10페이지 보기"
		                       href="list?page=${nextpage}"
		                       class="page-link page-number-link control next"
		                       data-page="${nextpage}">${nextpage}</a>
		                </li>
		            `;
					$pagination.append(nextContent);
				}

				let lastContent = `
		            <li class="page-item">
		                <a title="마지막 페이지 보기"
		                   href="list?page=${pagination.totalPages}"
		                   class="page-link page-number-link control last"
		                   data-page="${pagination.totalPages}">${pagination.totalPages}</a>
		            </li>
		        `;
				$pagination.append(lastContent);
			 }
			
		})
    
    }
    
    
    $("#table-notice tbody").on("click", "a", function(event) {
		event.preventDefault();
		
		let noticeNo = $(this).attr("data-no");
		$("#actionForm input[name=no]").val(noticeNo);
		$("#actionForm").attr("action", 'notice/detail?no=' + noticeNo);
		
		document.querySelector("#actionForm").submit();
	})
    
    
});









