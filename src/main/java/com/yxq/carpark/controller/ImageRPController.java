package com.yxq.carpark.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.istack.logging.Logger;
import com.yxq.carpark.dto.FormData;
import com.yxq.carpark.entity.ParkInfo;
import com.yxq.carpark.entity.Result;
import com.yxq.carpark.service.CouponService;
import com.yxq.carpark.service.DepotcardService;
import com.yxq.carpark.service.IllegalInfoService;
import com.yxq.carpark.service.IncomeService;
import com.yxq.carpark.service.ParkinfoService;
import com.yxq.carpark.service.ParkinfoallService;
import com.yxq.carpark.service.ParkspaceService;
import com.yxq.carpark.service.PlateRecognise;
import com.yxq.carpark.service.UserService;
import com.yxq.carpark.serviceImpl.PlateRecogniseImpl;
import com.yxq.carpark.utils.Access_token;
import com.yxq.carpark.utils.HttpUtil;
import com.yxq.carpark.utils.Msg;

@Controller
public class ImageRPController {
	
	private static final Logger logger = Logger.getLogger(ImageRPController.class);
	
	@Autowired
	private ParkinfoService parkinfoservice;
	@Autowired
	private ParkspaceService parkspaceService;
	@Autowired
	private DepotcardService depotcardService;
	@Autowired 
	private UserService userService;
	@Autowired
	private IllegalInfoService illegalInfoService;
	@Autowired
	private ParkinfoallService parkinfoallService;
	@Autowired
	private IncomeService incomeService;
	@Autowired
	private CouponService couponService;
	
	@RequestMapping(value = "/fileUpload")
	public String fileLpload() {

		return "upload";
	}
	@ResponseBody
	@RequestMapping(value = "/fileUpload2")
	public Result upload2(@RequestParam("file") MultipartFile file) {

	//	System.out.println(parkId);
		if (file.isEmpty() || file==null) {
			List<String> responses = new ArrayList<String>();
			responses.add("文件为空");
			logger.info("文件为空");
			return new Result(-1, responses, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		}
		String fileName = file.getOriginalFilename();
		@SuppressWarnings("unused")
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		String filePath = "C:\\springUpload\\image\\";
		// fileName = UUID.randomUUID() + suffixName;
		File dest = new File(filePath + fileName);
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
		try {
			file.transferTo(dest);
			PlateRecognise plateRecognise = new PlateRecogniseImpl();
			String img = filePath + fileName;
			logger.info(img);
			List<String> res = plateRecognise.plateRecognise(filePath + fileName);
			if (res.size() < 1 || res.contains("")) {
				logger.info("识别失败！不如换张图片试试？");
				List<String> responses = new ArrayList<String>();
				responses.add("识别不了");
				return new Result(-1, responses, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			}
			Result result = new Result(201, plateRecognise.plateRecognise(filePath + fileName),
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			logger.info(result.toString());
			return result;
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<String> responses = new ArrayList<String>();
		responses.add("上传失败");
	//	return parkInfo;
		return new Result(-1, responses, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
	}

	
	@RequestMapping(value = "/fileUpload1")
	public String upload(@RequestParam("file") MultipartFile file,@RequestParam("id")int id,HttpServletResponse response,HttpServletRequest request) {
		int parkId=id;
		ParkInfo parkInfo=new ParkInfo();
		FormData formData=new FormData();
		System.out.println(parkId);
	
		String fileName = file.getOriginalFilename();
		@SuppressWarnings("unused")
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		String filePath = "C:\\springUpload\\image\\";
		// fileName = UUID.randomUUID() + suffixName;
		File dest = new File(filePath + fileName);
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
	
			try {
				file.transferTo(dest);
				PlateRecognise plateRecognise = new PlateRecogniseImpl();
				String img = filePath + fileName;
				logger.info(img);
				List<String> res = plateRecognise.plateRecognise(filePath + fileName);
				if (res.size() < 1 || res.contains("")) {
					logger.info("识别失败！不如换张图片试试？");
					
					//return Msg.fail().add("va_msg", "密码错误");
					response.setHeader("refresh", "6;url="+request.getContextPath()+"/index/toindex");
					return "error";
					//response.setHeader("refresh", "5;url=/index/toindex");
					//return "redirect:/index/toindex";
				}
				String carNum=res.get(0);
				Result result = new Result(201, plateRecognise.plateRecognise(filePath + fileName),
						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				logger.info(result.toString());
				if (depotcardService.findCardnumByCarnum(carNum)!=null) {
					formData.setCardNum(depotcardService.findCardnumByCarnum(carNum));
					formData.setCarNum(carNum);
					formData.setParkNum(parkId);
					formData.setParkTem(0);
				}else {
					formData.setCardNum("");
					formData.setCarNum(carNum);
					formData.setParkNum(parkId);
					formData.setParkTem(1);
				}
				
				parkinfoservice.saveParkinfo(formData);
				parkspaceService.changeStatus(parkId, 1);
				//return "index";
				return "redirect:/index/toindex";
				//return Msg.success();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "redirect:/index/toindex";
	}
	
	
	
	@RequestMapping(value = "/fileUpload11")
	public String upload1(@RequestParam("file") MultipartFile file,@RequestParam("id")int id,HttpServletResponse response,HttpServletRequest request) {
		int parkId=id;
		ParkInfo parkInfo=new ParkInfo();
		FormData formData=new FormData();
		System.out.println(parkId);
	
		String fileName = file.getOriginalFilename();
		@SuppressWarnings("unused")
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		String filePath = "C:\\springUpload\\image\\";
		// fileName = UUID.randomUUID() + suffixName;
		File dest = new File(filePath + fileName);
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
	
			try {
				file.transferTo(dest);
				PlateRecognise plateRecognise = new PlateRecogniseImpl();
				String img = filePath + fileName;
				logger.info(img);
				//List<String> res = plateRecognise.plateRecognise(filePath + fileName);
				
				
				
				
				  Map<String,Object> map1 = new HashMap<>();
			        // 获取token
			        String accessToken = Access_token.getAuth();
			        // 通用识别url
			        String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";
			            //读取本地图片输入流
			            FileInputStream inputStream = new FileInputStream(filePath + fileName);
			            String base = Base64.encodeBase64String(IOUtils.toByteArray(inputStream));
			            logger.info(base);
			            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(base, "UTF-8");
			            /**
			             * 线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
			             */
//			            String accessToken = "#####调用鉴权接口获取的token#####";
			            String result = HttpUtil.post(otherHost, accessToken, params);
			            JSONObject jsonObject = JSONObject.parseObject(result);
			            JSONArray words_result = (JSONArray) jsonObject.get("words_result");
			            JSONObject o = (JSONObject)words_result.get(0);
			            //map1.put("lincens",o.get("words"));
			            //System.out.println(result);
			    
			            String carNum=(String) o.get("words");
			            logger.info("识别成功！车牌号码：" + carNum);
//				
//				if (res.size() < 1 || res.contains("")) {
//					logger.info("识别失败！不如换张图片试试？");
//					
//					//return Msg.fail().add("va_msg", "密码错误");
//					response.setHeader("refresh", "6;url="+request.getContextPath()+"/index/toindex");
//					return "error";
//					//response.setHeader("refresh", "5;url=/index/toindex");
//					//return "redirect:/index/toindex";
//				}
//				String carNum=res.get(0);
//				Result result = new Result(201, plateRecognise.plateRecognise(filePath + fileName),
//						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
//				logger.info(result.toString());
				if (depotcardService.findCardnumByCarnum(carNum)!=null) {
					formData.setCardNum(depotcardService.findCardnumByCarnum(carNum));
					formData.setCarNum(carNum);
					formData.setParkNum(parkId);
					formData.setParkTem(0);
				}else {
					formData.setCardNum("");
					formData.setCarNum(carNum);
					formData.setParkNum(parkId);
					formData.setParkTem(1);
				}
				
				parkinfoservice.saveParkinfo(formData);
				parkspaceService.changeStatus(parkId, 1);
				//return "index";
				return "redirect:/index/toindex";
				//return Msg.success();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return "redirect:/index/toindex";
	}
	
	@RequestMapping(value = "/fileUpload3")
	public String upload3(@RequestParam("file") MultipartFile file,@RequestParam("id")int id) throws Exception, IOException {
		int parkId=id;
		ParkInfo parkInfo=new ParkInfo();
		FormData formData=new FormData();
		System.out.println(parkId);
	
		String fileName = file.getOriginalFilename();
		@SuppressWarnings("unused")
		String suffixName = fileName.substring(fileName.lastIndexOf("."));
		String filePath = "C:\\springUpload\\image\\";
		// fileName = UUID.randomUUID() + suffixName;
		File dest = new File(filePath + fileName);
		if (!dest.getParentFile().exists()) {
			dest.getParentFile().mkdirs();
		}
	
			file.transferTo(dest);
			PlateRecognise plateRecognise = new PlateRecogniseImpl();
			String img = filePath + fileName;
			logger.info(img);
			List<String> res = plateRecognise.plateRecognise(filePath + fileName);
			String carNum=res.get(0);
			Result result = new Result(201, plateRecognise.plateRecognise(filePath + fileName),
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			logger.info(result.toString());
			if (depotcardService.findCardnumByCarnum(carNum)!=null) {
				formData.setCardNum(depotcardService.findCardnumByCarnum(carNum));
				formData.setCarNum(carNum);
				formData.setParkNum(parkId);
				formData.setParkTem(0);
			}else {
				formData.setCardNum("");
				formData.setCarNum(carNum);
				formData.setParkNum(parkId);
				formData.setParkTem(1);
			}
			
			parkinfoservice.saveParkinfo(formData);
			parkspaceService.changeStatus(parkId, 1);
			//return "index";
			return "redirect:/index/toindex";
			//return Msg.success();
	
	}
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/plateRecognise")
	public List<String> plateRecognise(@RequestParam("imgPath") String imgPath) {
		PlateRecognise plateRecognise = new PlateRecogniseImpl();
		return plateRecognise.plateRecognise(imgPath);
	}
	
	/**
     * 获取百度AI应用数据.
     *
     * @return message
     */
    @RequestMapping(value = "/baidu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map getAisss() {
        Map<String,Object> map1 = new HashMap<>();
        // 获取token
        String accessToken = Access_token.getAuth();
        // 通用识别url
        String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";
        try {
            //读取本地图片输入流
            FileInputStream inputStream = new FileInputStream("E://test1.jpg");
            String base = Base64.encodeBase64String(IOUtils.toByteArray(inputStream));
            logger.info(base);
            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(base, "UTF-8");
            /**
             * 线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
             */
//            String accessToken = "#####调用鉴权接口获取的token#####";
            String result = HttpUtil.post(otherHost, accessToken, params);
            JSONObject jsonObject = JSONObject.parseObject(result);
            JSONArray words_result = (JSONArray) jsonObject.get("words_result");
            JSONObject o = (JSONObject)words_result.get(0);
            map1.put("lincens",o.get("words"));
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
 
        return map1;
    }
}
