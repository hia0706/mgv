package kr.co.mgv.theater;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kr.co.mgv.theater.location.Location;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/theater")
@RequiredArgsConstructor
public class TheaterController {

	private final TheaterService theaterService;
	
    @GetMapping({"/", ""})
    public String home() {
        return "/view/theater/home";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam(defaultValue = "1") int brchNo, Model model) {
    	Theater theater = theaterService.getTheaterDetail(brchNo);
    	String[] parkingcashs = theater.getParkingInfo().getCash().split("\\n");
    	model.addAttribute("theater", theater);
    	model.addAttribute("parkingcashs", parkingcashs);
        return "/view/theater/detail";
    }
    
    @GetMapping("/theaterList")
    @ResponseBody
    public List<Location> theaterList(){
    	List<Location> locations = theaterService.getTheaters();
    	return locations;
    }

}
