package kr.co.mgv.user.controller;

import kr.co.mgv.booking.service.BookingService;
import kr.co.mgv.booking.vo.Booking;
import kr.co.mgv.store.mapper.OrderMapper;
import kr.co.mgv.store.service.OrderService;
import kr.co.mgv.store.vo.GiftTicket;
import kr.co.mgv.user.form.UserUpdateForm;
import kr.co.mgv.user.service.MypageService;
import kr.co.mgv.user.service.UserService;
import kr.co.mgv.user.vo.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Secured({"ROLE_USER", "ROLE_ADMIN"})
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final MypageService mypageService;
    private final PasswordEncoder passwordEncoder;
    private final OrderService orderService;
    private final BookingService bookingService;

    private String getLoggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @RequestMapping({"/", ""})
    public String home(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", userService.getUserById(user.getId()));
        model.addAttribute("bookings",bookingService.getBookingsByUserId(user.getId()).stream().filter(b->"결제완료".equals(b.getBookingState())).collect(Collectors.toList()));
        model.addAttribute("orders", mypageService.getOrderByUserId(user.getId()).stream().filter(order -> "결제완료".equals(order.getState())).collect(Collectors.toList()));
        return "view/user/home";
    }

    // 이메일 인증
    @GetMapping("/auth")
    public String emailAuthForm(@AuthenticationPrincipal User user, Model model) {

        model.addAttribute("user", user);

        return "view/user/info/auth";
    }

    @PostMapping("/auth")
    public String success() {
        return "redirect:/mypage/form";
    }

    // 회원정보 수정
    @GetMapping("/form")
    public String myMGV(@AuthenticationPrincipal User user, Model model) {
        user = userService.getUserById(user.getId());
        long minDate = userService.getMinDate(user.getUpdateDate());
        long pwdMinDate = userService.getMinDate(user.getPwdUpdateDate());

        model.addAttribute("user", user);
        model.addAttribute("minDate", minDate);
        model.addAttribute("pwdMinDate", pwdMinDate);

        return "view/user/info/form";
    }

    // 회원정보 수정
    @PostMapping("/update")
    public ResponseEntity<String> updateUser(@AuthenticationPrincipal User user, UserUpdateForm form) {
        // 만약 이메일을 수정하지 않는 경우
        if (form.getEmail().equals(user.getEmail())) {
            userService.updateUser(user.getId(), form.getEmail(), form.getZipcode(), form.getAddress());
            return ResponseEntity.ok("이메일 수정 안함");
        }

        // 이메일을 수정하는 경우
        User checkEmail = userService.getUserByEmail(form.getEmail());

        if (checkEmail == null || checkEmail.getId().equals(user.getId())) {
            userService.updateUser(user.getId(), form.getEmail(), form.getZipcode(), form.getAddress());
            return ResponseEntity.ok("가능");
        } else {
            return ResponseEntity.badRequest().body("중복된 이메일 주소입니다.");
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> updateUploadImg(@AuthenticationPrincipal User user, UserUpdateForm form) {
        try {
            String newImgPath = userService.updateUploadProfile(user.getId(), form.getFile());
            log.info("Controller - inputFileName -> {}", newImgPath);
            return ResponseEntity.ok(newImgPath);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/deleteImg")
    @ResponseBody
    public ResponseEntity<?> deleteImg(@AuthenticationPrincipal User user, String file) {
        try {
            userService.deleteProfileImg(user.getId(), file);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("서버 에러", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 비밀번호 변경
    @GetMapping("/update/password")
    public String pwdForm() {
        return "view/user/info/pwdForm";
    }

    @PostMapping("/update/password")
    public ResponseEntity<String> updatePwd(@AuthenticationPrincipal User user, UserUpdateForm form) {
        if (passwordEncoder.matches(form.getCheckPassword(), user.getPassword())) {
            userService.updatePassword(user.getId(), passwordEncoder.encode(form.getNewPassword()));
            return ResponseEntity.ok("비밀번호가 변경되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("현재 비밀번호가 일치하지 않습니다.");
        }
    }

    // 회원 탈퇴
    @GetMapping("/disabled")
    public String disableForm(@AuthenticationPrincipal User user, Model model) {

        model.addAttribute("user", user);

        return "view/user/info/disabled";
    }

    // 사용자 이메일 체크
    @PostMapping("/checkEmail")
    public ResponseEntity<String> checkEmail(@AuthenticationPrincipal User user, UserUpdateForm form) {
        if (form.getEmail().equals(user.getEmail())) {
            return ResponseEntity.ok(user.getEmail());
        } else {
            return ResponseEntity.badRequest().body("등록된 이메일 주소와 일치하지 않습니다.");
        }
    }

    // 회원 탈퇴
    @PostMapping("/disabled")
    public ResponseEntity<String> disableUser(@AuthenticationPrincipal User user, UserUpdateForm form) {
        if (passwordEncoder.matches(form.getCheckPassword(), user.getPassword())) {
            userService.disableUser(user.getId(), form.getReason());

            return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("현재 비밀번호가 일치하지 않습니다.");
        }
    }

    @GetMapping("/booking")
    public String bookinghome(@AuthenticationPrincipal User user, Model model) {
        List<Booking> bookingAll = bookingService.getBookingsByUserId(user.getId());
        
        model.addAttribute("bookings",bookingAll.stream().filter(b->"결제완료".equals(b.getBookingState())).collect(Collectors.toList()));
        model.addAttribute("bookingCancels",bookingAll.stream().filter(b->"예매취소".equals(b.getBookingState())).collect(Collectors.toList()));
        model.addAttribute("totalRows", bookingService.getTotalRows(user.getId()));
        return "view/user/booking/list";
    }

    @PostMapping("/order")
    @ResponseBody
    public ResponseEntity<HashMap<String, Object>> OrderList(@RequestParam String startDate,
                                                             @RequestParam String endDate,
                                                             @RequestParam String state,
                                                             @RequestParam(name = "page", defaultValue = "1") int page) {
        String userId = getLoggedInUserId();
        log.info("loginId -> {}", userId);

        String endDateWithTime = endDate + " 23:59:59";
        log.info("Converted endDate -> {}", endDateWithTime);

        HashMap<String, Object> orders = mypageService.getOrders(userId, startDate, endDateWithTime, state, page);
        log.info("page -> {}", page);
        log.info("startDate -> {}", startDate);
        log.info("endDate -> {}", endDateWithTime);
        log.info("state -> {}", state);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/order/cancel")
    @ResponseBody
    public ResponseEntity<String> cancelPurchace(@RequestParam("orderId") long orderId) {
        boolean isSuccess = mypageService.cancelOrder(orderId);
        log.info("orderId -> {}", orderId);
        if (isSuccess) {
            return ResponseEntity.ok("결제취소가 완료되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("구매취소 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    @GetMapping("/ticket")
    public String ticketList(@AuthenticationPrincipal User user, Model model) {
        List<GiftTicket> giftTickets = orderService.getGiftTicketsByUserId(user.getId());

        for (GiftTicket ticket : giftTickets) {
            Calendar c = Calendar.getInstance();
            c.setTime(ticket.getCreateDate());
            c.add(Calendar.YEAR, 1);
            ticket.setExpiryDate(c.getTime());
        }

        model.addAttribute("giftTickets", giftTickets);
        return "view/user/ticket/list";
    }

    @GetMapping("/moviestory")
    public String moviestory() {
        return "view/user/moviestory/list";
    }

    @GetMapping("/inquiry")
    public String inquiry() {

        return "view/user/inquiry/list";
    }

}
