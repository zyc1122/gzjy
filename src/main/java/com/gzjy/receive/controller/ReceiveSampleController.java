/**
 * @author xuewenlong@cmss.chinamobile.com
 * @updated 2017年9月3日
 */
package com.gzjy.receive.controller;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageInfo;
import com.gzjy.common.Add;
import com.gzjy.common.Response;
import com.gzjy.common.ShortUUID;
import com.gzjy.common.Update;
import com.gzjy.common.annotation.Privileges;
import com.gzjy.common.exception.BizException;
import com.gzjy.common.util.ExcelToPdf;
import com.gzjy.common.util.FileOperate;
import com.gzjy.common.util.fs.EpicNFSClient;
import com.gzjy.common.util.fs.EpicNFSService;
import com.gzjy.receive.model.ReceiveSample;
import com.gzjy.receive.model.ReceiveSampleItem;
import com.gzjy.receive.service.ReceiveSampleService;
import com.gzjy.user.UserService;
import com.gzjy.user.model.User;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.OutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
//import xue.model.StudentBeanFactory;
//import xue.model.StudentScore;

/**
 * @author xuewenlong@cmss.chinamobile.com
 * @updated 2017年9月3日
 */
@RestController
@RequestMapping(value = "v1/ahgz/receive")
public class ReceiveSampleController {

	private static Logger logger = LoggerFactory.getLogger(ReceiveSampleService.class);

	@Autowired
	private ReceiveSampleService receiveSampleService;

	@Autowired
	private EpicNFSService epicNFSService;
	@Autowired
	private UserService userService;
	 @Autowired
	 private DataSource dataSource; 
	@RequestMapping(value = "/sample/xue_test", method = RequestMethod.GET)
	public void testReport() throws Exception{
	    //设定报表所需要的外部参数内容
	    Map<String, Object> rptParameters = new HashMap<String, Object>();
	       //rptParameters.put("name", "薛文龙");
	      // rptParameters.put("age", "12");
	    rptParameters.put("receiveSampleId", "F22121");
	       
	       //传入报表源文件绝对路径，外部参数对象，DB连接，得到JasperPring对象
	      // JasperPrint jasperPrint = JasperFillManager.fillReport("F:/Blank_A4.jasper", rptParameters, new JREmptyDataSource());
	       JasperPrint jasperPrint = JasperFillManager.fillReport("E:/var/lib/docs/gzjy/template/standard.jasper", rptParameters, dataSource.getConnection());
	       
	       JasperExportManager.exportReportToHtmlFile(jasperPrint,"F:/standard.html");
	      
	       JasperExportManager.exportReportToPdfFile(jasperPrint, "F:/standard.xml");
	       String xmlexport =JasperExportManager.exportReportToXml(jasperPrint);
	       System.out.println(xmlexport);
	       //导出PDF文件
	       JasperExportManager.exportReportToPdfFile(jasperPrint, "F:/standard.pdf");
	       
	       //导入HTML文件
	      // JasperExportManager.exportReportToHtmlFile(jasperPrint, "D:/temp/jasper_test/test.html");
	       
	       //执行结束
	       System.out.println("Export success!!");
	       //使用javabean作为数据源
	      /* ArrayList<StudentScore> stulist=new ArrayList<StudentScore>();
	       stulist=StudentBeanFactory.getBeanCollection();
	       Map<String, Object> par = new HashMap<String, Object>();
	       JRDataSource data= new JRBeanCollectionDataSource(stulist);  
	       JasperPrint jasperPrint1 = JasperFillManager.fillReport("F:/412.jasper", par,data);
	       JasperExportManager.exportReportToHtmlFile(jasperPrint1,"F:/a412.html");*/
     
	}

	// 添加接样基本信息
	@RequestMapping(value = "/sample", method = RequestMethod.POST)
	@Privileges(name = "SAMPLE-ADD", scope = { 1 })
	public Response addSample(@Validated(value = { Add.class }) @RequestBody ReceiveSample receiveSample,
			BindingResult result) {
		if (result.hasErrors()) {
			return Response.fail(result.getFieldError().getDefaultMessage());
		}
		if (StringUtils.isBlank(receiveSample.getReceiveSampleId())) {
			return Response.fail("接收样品编号不能为空");
		}
		if (StringUtils.isBlank(receiveSample.getReportId())) {
            return Response.fail("报告编号不能为空");
        }
		Boolean flag = receiveSampleService.addReceiveSample(receiveSample);
		return Response.success(flag);
	}
	
	   // 更新接样基本信息
    @Privileges(name = "SAMPLE-UPDATESAMPLE", scope = { 1 })
    @RequestMapping(value = "/sample", method = RequestMethod.PUT)
    public Response updateSample(@Validated(value = { Update.class }) @RequestBody ReceiveSample receiveSample,
            BindingResult result) {
        if (result.hasErrors()) {
            return Response.fail(result.getFieldError().getDefaultMessage());
        }
        if (StringUtils.isBlank(receiveSample.getReceiveSampleId())) {
            return Response.fail("接收样品编号不能为空");
        }
        if (StringUtils.isBlank(receiveSample.getReportId())) {
            return Response.fail("报告编号不能为空");
        }
        receiveSample = receiveSampleService.updateReceiveSample(receiveSample);
        
        return Response.success(receiveSample);
    }

	// 变更接样中检验项基本信息（添加检验项）
	@Privileges(name = "SAMPLE-ADDITEM", scope = { 1 })
	@RequestMapping(value = "/sample/item/add/{reportId}", method = RequestMethod.POST)
	public Response addItem(@PathVariable("reportId") String reportId,
			@Validated(value = { Add.class }) @RequestBody List<ReceiveSampleItem> items, BindingResult result) {
		if (result.hasErrors()) {
			return Response.fail(result.getFieldError().getDefaultMessage());
		}
		for (ReceiveSampleItem item : items) {
			if (!item.getReportId().equals(reportId)) {
				return Response.fail("报告编号ID存在异常");
			}
		}
		boolean flag = receiveSampleService.addReceiveSampleItems(items);

		return Response.success("操作成功：" + flag);
	}
	
	// 更新接样中检验项基本信息
	//@Privileges(name = "SAMPLE-ADDITEM", scope = { 1 })
    @RequestMapping(value = "/sample/item/update/{reportId}", method = RequestMethod.POST)
    public Response updateItem(@PathVariable("reportId") String reportId,
            @Validated(value = { Update.class }) @RequestBody List<ReceiveSampleItem> items, BindingResult result) {
        if (result.hasErrors()) {
            return Response.fail(result.getFieldError().getDefaultMessage());
        }
        for (ReceiveSampleItem item : items) {
            if (!item.getReportId().equals(reportId)) {
                return Response.fail("报告编号ID存在异常");
            }
        }
        boolean flag = receiveSampleService.addReceiveSampleItems(items);

        return Response.success("操作成功：" + flag);
    }
    
    
    /**
     * 
     * @param items   传ReceiveSampleItem的uuid
     * @param result
     * @return
     * ReceiveSampleController.java
     */
  //批量分配检测项
    @RequestMapping(value = "/sample/item/distribute", method = RequestMethod.POST)
    public Response distributeItems(@RequestBody List<ReceiveSampleItem> items, BindingResult result) {
        if (result.hasErrors()) {
            return Response.fail(result.getFieldError().getDefaultMessage());
        }
        boolean flag = receiveSampleService.addReceiveSampleItems(items);

        return Response.success("操作成功：" + flag);
    }
    
    
	
	
	//设置检测项结果（编辑）
	@Privileges(name = "SAMPLE-UPDATEITEMRESULT", scope = { 1 })
	@RequestMapping(value = "/sample/item/result", method = RequestMethod.POST)
	public Response setItemResoult(@Validated(value = { Update.class }) @RequestBody List<ReceiveSampleItem> items,
			BindingResult result) {
		if (result.hasErrors()) {
			return Response.fail(result.getFieldError().getDefaultMessage());
		}
		
		boolean flag = receiveSampleService.updateSampleItemsResoult(items);
		/*for (ReceiveSampleItem item : items) {
		    if(receiveSampleService.checkReceiveSampleIsFinished(item.getReportId())) { //如果接样单的检测项都完成了结果录入
		        receiveSampleService.setStatus(item.getReportId(), 1);
		    }
        }*/

		return Response.success("操作成功：" + flag);
	}
	
	
	//设置检测项结果状态为完成
    @Privileges(name = "SAMPLE-UPDATEITEMRESULT", scope = { 1 })
    @RequestMapping(value = "/sample/item/result/finish", method = RequestMethod.POST)
    public Response setItemResoultFinish( @RequestBody List<String> items,
            BindingResult result) {
        if (result.hasErrors()) {
            return Response.fail(result.getFieldError().getDefaultMessage());
        }
        boolean flag=receiveSampleService.setSampleItemsFinish(items);
        
        for (String item : items) {
           ReceiveSampleItem sampleItem= receiveSampleService.getItem(item);
            if(receiveSampleService.checkReceiveSampleIsFinished(sampleItem.getReportId())) { //如果接样单的检测项都完成了结果录入               
                receiveSampleService.setStatus(sampleItem.getReportId(), 1);
            }
        }

        return Response.success("操作成功：" + flag);
    }
	
	

	// 删除接样单（包括接样单中的检验项）
	@Privileges(name = "SAMPLE-DELETESAMPLE", scope = { 1 })
	@RequestMapping(value = "/sample/delete", method = RequestMethod.POST)
	public Response deleteSample(@RequestBody List<String> reportIds, BindingResult result) {
		String record = null;
		if (result.hasErrors()) {
			return Response.fail(result.getFieldError().getDefaultMessage());
		}

		for (String id : reportIds) {
			receiveSampleService.delete(id);
			record += id + "、";
		}

		record = record.substring(0, record.length() - 1);
		return Response.success(record + "删除成功：");
	}

	// 删除接样中检验项基本信息（删除检验项时直接调用后台接口删除）
	@Privileges(name = "SAMPLE-DELETEITEM", scope = { 1 })
	@RequestMapping(value = "/sample/items/{reportId}/delete", method = RequestMethod.POST)
	public Response deleteItem(@PathVariable("reportId") String reportId, @RequestBody List<String> items,
			BindingResult result) {
		if (result.hasErrors()) {
			return Response.fail(result.getFieldError().getDefaultMessage());
		}
		for (String id : items) {
			ReceiveSampleItem itemRecord = receiveSampleService.getItem(id);
			if (!itemRecord.getReportId().equals(reportId)) {
				throw new BizException("传递了一个错误的检验项ID");
			}
		}

		return Response.success(receiveSampleService.deleteReceiveSampleItems(items));
	}



	// 查询接样信息
	@RequestMapping(value = "/sample", method = RequestMethod.GET)
	@Privileges(name = "SAMPLE-SELECT", scope = { 1 })
	public Response list(@RequestParam(name = "receiveSampleId", required = false) String receiveSampleId,
	        @RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			@RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime) {
		Map<String, Object> filter = new HashMap<String, Object>();

		if (StringUtils.isBlank(startTime)) {
			startTime = null;
		}
		if (StringUtils.isBlank(endTime)) {
		    endTime=null;
        }
		if(!StringUtils.isBlank(reportId)) {
		    filter.put("report_id", reportId);
		}
		if(!StringUtils.isBlank(inspectedUnit)) {
            filter.put("inspected_unit", inspectedUnit);
        }
		if(!StringUtils.isBlank(sampleName)) {
            filter.put("sample_name", sampleName);
        }
		if(!StringUtils.isBlank(executeStandard)) {
            filter.put("execute_standard", executeStandard);
        }
		if(!StringUtils.isBlank(productionUnit)) {
            filter.put("production_unit", productionUnit);
        }
		if (!StringUtils.isBlank(receiveSampleId)) {
			filter.put("receive_sample_id", receiveSampleId);
		}
		if (!StringUtils.isBlank(entrustedUnit)) {
			filter.put("entrusted_unit", entrustedUnit);
		}
		if (!StringUtils.isBlank(sampleType)) {
			filter.put("sample_type", sampleType);
		}
		if (!StringUtils.isBlank(checkType)) {
            filter.put("check_type", checkType);
        }
		if (reportStatus!=null) {
            filter.put("report_status", reportStatus);
        }
		if (StringUtils.isBlank(order)) {
			order = "created_at desc";
		}
		if (status != 5) {
			filter.put("status", status);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date start = null;
		Date end = null;

		try {
			start = startTime == null ? null : sdf.parse(startTime);
			end = endTime == null ? null : sdf.parse(endTime);
		} catch (Exception e) {
			e.printStackTrace();
			throw new BizException("输入的时间格式不合法！");
		}

		return Response.success(receiveSampleService.select(pageNum, pageSize, order, filter, start, end));
	}
	
	
	
	
	// 查询已经下发检测项任务到科室的接样信息
    @RequestMapping(value = "/sample/listForDistribute", method = RequestMethod.GET)
    @Privileges(name = "SAMPLE-DISTRIBUTE-SELECT", scope = { 1 })
    public Response listSampleForDistribute(@RequestParam(name = "receiveSampleId", required = false) String receiveSampleId,
            @RequestParam(name = "reportId", required = false) String reportId,
            @RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
            @RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
            @RequestParam(name = "sampleName", required = false) String sampleName,
            @RequestParam(name = "executeStandard", required = false) String executeStandard,
            @RequestParam(name = "productionUnit", required = false) String productionUnit,
            @RequestParam(name = "sampleType", required = false) String sampleType,
            @RequestParam(name = "checkType", required = false) String checkType,
            @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "status", defaultValue = "-1") int status,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        Map<String, Object> filter = new HashMap<String, Object>();

        if (StringUtils.isBlank(startTime)) {
            startTime = null;
        }
        if (StringUtils.isBlank(endTime)) {
            endTime=null;
        }
        if(!StringUtils.isBlank(reportId)) {
            filter.put("report_id", reportId);
        }
        if(!StringUtils.isBlank(inspectedUnit)) {
            filter.put("inspected_unit", inspectedUnit);
        }
        if(!StringUtils.isBlank(sampleName)) {
            filter.put("sample_name", sampleName);
        }
        if(!StringUtils.isBlank(executeStandard)) {
            filter.put("execute_standard", executeStandard);
        }
        if(!StringUtils.isBlank(productionUnit)) {
            filter.put("production_unit", productionUnit);
        }
        if (!StringUtils.isBlank(receiveSampleId)) {
            filter.put("receive_sample_id", receiveSampleId);
        }
        if (!StringUtils.isBlank(entrustedUnit)) {
            filter.put("entrusted_unit", entrustedUnit);
        }
        if (!StringUtils.isBlank(sampleType)) {
            filter.put("sample_type", sampleType);
        }
        if (!StringUtils.isBlank(checkType)) {
            filter.put("check_type", checkType);
        }
        if (reportStatus!=null) {
            filter.put("report_status", reportStatus);
        }
        if (StringUtils.isBlank(order)) {
            order = "created_at desc";
        }
        if (status != 5) {
            filter.put("status", status);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = null;
        Date end = null;

        try {
            start = startTime == null ? null : sdf.parse(startTime);
            end = endTime == null ? null : sdf.parse(endTime);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("输入的时间格式不合法！");
        }
        if(pageNum==null) {
            return Response.success(receiveSampleService.selectNoPage(order, filter, start, end));
        }else {
            return Response.success(receiveSampleService.select(pageNum, pageSize, order, filter, start, end));
        }

        
    }
    
    
    //查询当前用户本部门所有未分配的检测项
    @RequestMapping(value = "/sample/items/assign", method = RequestMethod.GET)
    @Privileges(name = "SAMPLE-DISTRIBUTE-SELECT", scope = { 1 })
    public Response getDepartmentItems(@RequestParam(name = "order", required = false) String order,
           @RequestParam(name = "reportId", required = false) String reportId,
           @RequestParam(name = "name", required = false) String name,   //项目名称
           @RequestParam(name = "method", required = false) String method,
           @RequestParam(name = "status", defaultValue = "-1") Integer status,  //5代表所有
           @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Map<String, Object> filter = new HashMap<String, Object>();
        if(!StringUtils.isBlank(reportId)) {
            filter.put("report_id", reportId);
        }
        if(!StringUtils.isBlank(name)) {
            filter.put("name", name);
        }
        if(!StringUtils.isBlank(method)) {
            filter.put("method", method);
        }
        if (status != 5) {
            filter.put("status", status);
        }
        if (StringUtils.isBlank(order)) {
            order = "updated_at desc";
        }
        return Response.success(receiveSampleService.selectUnderDepartmentReceiveSampleItems(pageNum, pageSize, order, filter));
    }
    
	
	// test
	@RequestMapping(value = "/sampletest", method = RequestMethod.GET)
	public Response list(@RequestParam(name = "entrustedunit", required = false) String entrustedUnit,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		Map<String, Object> filter = new HashMap<String, Object>();

		filter.put("check_type", "抽检");

		return Response.success(receiveSampleService.selectTest(pageNum, pageSize, filter));
	}
	
	//按照部门查询每种状态的检测项数量
    @RequestMapping(value = "/sample/items/countByDepartment", method = RequestMethod.GET)
    public Response selectCountItemByDepartment(@RequestParam(name = "order", required = false) String order,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime){
        if (StringUtils.isBlank(order)) {
            order = "updated_at desc";
        }
        Map<String, Object> filter = new HashMap<String, Object>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = null;
        Date end = null;
        
        try {
            start = startTime == null ? null : sdf.parse(startTime);
            end = endTime == null ? null : sdf.parse(endTime);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("输入的时间格式不合法！");
        }
        if(start==null) {
            start=new Date();
        }
        if(end==null) {
            end=new Date(new Date().getTime()-1592000000);//某人一个月前
        }
        if(start.after(end))
            throw new BizException("输入的时间格式不合法,开始时间大于了结束时间");
        
        
        return Response.success(receiveSampleService.selectCountItemByDepartment(filter, order, start, end));
        
    }
	// 根据报告ID获取接样信息
	@RequestMapping(value = "/sample/{reportId}", method = RequestMethod.GET)
	public Response getReceiveSample(@PathVariable(name = "reportId") String reportId) {

		return Response.success(receiveSampleService.getReceiveSample(reportId));
	}

	// 根据报告ID获得接样对应的检验项信息列表
	@RequestMapping(value = "/sample/items/{reportId}", method = RequestMethod.GET)
	public Response getItems(@PathVariable(name = "reportId") String reportId) {

		return Response.success(receiveSampleService.getItemsByReportId(reportId));
	}
	

	
	

	// 根据检测项ID获得单个检验项信息
	@RequestMapping(value = "/sample/items/item/{id}", method = RequestMethod.GET)
	public Response getItem(@PathVariable(name = "id") String itemId) {

		return Response.success(receiveSampleService.getItem(itemId));
	}

	// 设置接样单的状态
	@RequestMapping(value = "/sample/{reportId}/status/{status}", method = RequestMethod.GET)
	public Response setSampleStatus(@PathVariable(name = "reportId") String reportId,
			@PathVariable(name = "status") Integer status) {

		return Response.success(receiveSampleService.setStatus(reportId, status));
	}
	// 批量设置接样单的状态
	@RequestMapping(value = "/sample/status/{status}", method = RequestMethod.POST)
    public Response batchSetSampleStatus( @RequestBody List<String> reportIds,
            @PathVariable(name = "status") Integer status) {
	    boolean result=true;
	    for(String reportId:reportIds) {
	       boolean flag= receiveSampleService.setStatus(reportId, status);
	       if(!flag) {
	           result=flag;
	       }
	    }

        return Response.success(result);
    }

	// 查询当前用户检验项信息(检测人员关心的检验项)	
	@RequestMapping(value = "/sampleItem", method = RequestMethod.GET)
	@Privileges(name = "SAMPLE-ITEMS-INPUT-SELECT", scope = { 1 })
	public Response listItemByCurrentUser(@RequestParam(name = "status", defaultValue = "0") int status,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "receiveSampleId", required = false) String receiveSampleId,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "method", required = false) String method,
			@RequestParam(name = "receive_status", required = false) String receiveStatus,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		Map<String, Object> filter = new HashMap<String, Object>();
		if (status != 5) {
			filter.put("status", status);
		}
		if(!StringUtils.isBlank(receiveStatus)) {
		    filter.put("receivesample_status", receiveStatus);
		}
		if (StringUtils.isBlank(order)) {
			order = "updated_at desc";
		}
		if (!StringUtils.isBlank(receiveSampleId)) {
		    filter.put("receive_sample_id", receiveSampleId);
        }
		if (!StringUtils.isBlank(reportId)) {
            filter.put("report_id", reportId);
        }
		if (!StringUtils.isBlank(sampleName)) {
            filter.put("sample_name", sampleName);
        }
		if (!StringUtils.isBlank(name)) {
            filter.put("name", name);
        }
		if (!StringUtils.isBlank(method)) {
            filter.put("method", method);
        }
		PageInfo<ReceiveSampleItem> items=receiveSampleService.selectCurrentUserItems(pageNum, pageSize, order, filter);
		
		
		return Response.success(items);
	}
	
	
	
	// 查询当前用户接样单信息(检测人员关心的接样单)
    @RequestMapping(value = "/sampleItem/sample", method = RequestMethod.GET)
    @Privileges(name = "SAMPLE-ITEMS-INPUT-SELECT", scope = { 1 })
    public Response listReceiveItemByCurrentUser(@RequestParam(name = "status", defaultValue = "-1") int status,
            @RequestParam(name = "order", required = false) String order,
             @RequestParam(name = "receiveSampleId", required = false) String receiveSampleId,
            @RequestParam(name = "reportId", required = false) String reportId,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Map<String, Object> filter = new HashMap<String, Object>();
        if (status != 5) {
            filter.put("status", status);
        }
        if (StringUtils.isBlank(order)) {
            order = "finish_date asc";
        }
        if (!StringUtils.isBlank(receiveSampleId)) {
            filter.put("receive_sample_id", receiveSampleId);
        }
        if (!StringUtils.isBlank(reportId)) {
            filter.put("report_id", reportId);
        }
        if(pageNum==null) {
            return Response.success(receiveSampleService.selectUnderDetectionForNOPage(order, filter));
        }
        else
            return Response.success(receiveSampleService.selectUnderDetection(pageNum, pageSize, order, filter));
    }
    
    
 // 上传
    @RequestMapping(value = "/attachment/upload", method = RequestMethod.POST)
    public void uploadReceiveAppendix(@RequestParam("file") MultipartFile file,
             @RequestParam(name = "reportId", required = false) String reportId            
            ) {
        
            try {
                receiveSampleService.upload(file, reportId);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally {
                try {
                    if(file.getInputStream()!=null) {
                        file.getInputStream().close();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                    
            }
               
    }
    
    
    //删除附件
    @RequestMapping(value = "/attachment/delete", method = RequestMethod.DELETE)
    public void deleteReceiveAppendix(
             @RequestParam(name = "reportId", required = false) String reportId            
            ) {
       ReceiveSample sample=receiveSampleService.getReceiveSample(reportId);
       try {
        receiveSampleService.deleteAttachment(sample.getAppendix());
    } catch (Exception e) {
        e.printStackTrace();
    }         
    }
    
    // 下载
    @RequestMapping(value = "/attachment/download", method = RequestMethod.GET)
    public Response download(  @RequestParam(name = "path", required = true)String path,HttpServletResponse response           
            ) {
        try {
              response.reset();
              receiveSampleService.download(path, response);
          } catch (IOException e) {
        
          }
        return Response.success(true);
    }
        
 

	/**
	 * 通过模板导出报告(excel or pdf)并下载
	 * 
	 * @param response
	 * @param id
	 * @return
	 */
	/*@RequestMapping(value = "/sample/items/report/{id}", method = RequestMethod.GET)
	public Response getReport(HttpServletResponse response, @PathVariable(name = "id") String id,
			@RequestParam(required = true) String templateFileName, @RequestParam(required = true) String type) {
		EpicNFSClient client = epicNFSService.getClient("gzjy");
		// 生成临时模板excel文件
		String fileSuffix = templateFileName.endsWith("xlsx") ? ".xlsx" : ".xls";
		String temp = ShortUUID.getInstance().generateShortID();
		String tempFileName = temp + fileSuffix;
		// 建立远程存放excel模板文件目录
		if (!client.hasRemoteDir("temp")) {
			client.createRemoteDir("temp");
		}
		// 服务器模板文件存放目录
		String serverTemplatePath = "/var/lib/docs/gzjy/template/";
		// 根据接口参数得到服务器模板文件的实际路径
		String serverTemplateFile = serverTemplatePath + templateFileName;
		// 建立服务器缓存模板文件
		String tempFile = "/var/lib/docs/gzjy/temp/" + tempFileName;
		OutputStream out = null;
		String tempPdf = null;
		Workbook wb = null;
		try {
			// 将模板文件复制到缓存文件
			FileOperate.copyFile(serverTemplateFile, tempFile);
			// 获取报告数据
			ReceiveSample receiveSample = receiveSampleService.getReceiveSample(id);
			InputStream input = new FileInputStream(tempFile);
			if (fileSuffix.equals(".xlsx")) {
				wb = new XSSFWorkbook(input);
			} else {
				wb = new HSSFWorkbook(input);
			}
			// 将数据写入流中
			receiveSampleService.generateExcel(wb, receiveSample);
			if (type.equals("excel")) {
				// 如果是excel，则提供下载功能，需设置头信息
				response.reset();
				response.setContentType("application/octet-stream;charset=UTF-8");
				response.setHeader("Content-disposition",
						"attachment;filename=" + URLEncoder.encode(tempFileName, "UTF-8"));
				out = response.getOutputStream();
				wb.write(out);
				input.close();
			} else {
				logger.info("Begin export PDF");
				OutputStream outputStream = new FileOutputStream(tempFile);
				wb.write(outputStream);				
				String tempPdfName = ShortUUID.getInstance().generateShortID() + ".pdf";
				tempPdf = "/var/lib/docs/gzjy/temp/" + tempPdfName;
				String os = System.getProperty("os.name");
				if (os.toLowerCase().startsWith("win")) {
					ExcelToPdf.xlsToPdf(tempFile, tempPdf);
				} else {
					ExcelToPdf.xlsToPdfForLinux(tempFile);
				}
				logger.info("End export PDF");
				// 删除缓存模板文件
				FileOperate.deleteFile(tempFile);
				return Response.success("/var/lib/docs/gzjy/temp/"+temp+".pdf");
			}	
			// 删除缓存模板文件
			FileOperate.deleteFile(tempFile);
		} catch (Exception e) {
			logger.error(e + "");
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	*//**
	 * 
	 * @param response
	 * @param id
	 * @return
	 *//*
	@RequestMapping(value = "/sample/export", method = RequestMethod.GET)
	public Response exportExcel(HttpServletResponse response, @RequestParam(required = false) String templateFileName) {
		ReceiveSample receiveSample = new ReceiveSample();
		List<HashMap<String, String>> data = receiveSampleService.selectAllItem(receiveSample);
		// 建立服务器缓存模板文件
		String tempFileName = ShortUUID.getInstance().generateShortID() + ".xlsx";
		String tempFile = "/var/lib/docs/gzjy/temp/" + tempFileName;
		OutputStream out = null;
		Workbook wb = null;
		try {
			File file = new File(tempFile);
			if(!file.exists()) {
				file.createNewFile();
			}			
			wb = new XSSFWorkbook();
			// 将数据写入流中
			receiveSampleService.generateExcelReport(wb, data);
			// 如果是excel，则提供下载功能，需设置头信息
			response.reset();
			response.setContentType("application/octet-stream;charset=UTF-8");
			response.setHeader("Content-disposition",
					"attachment;filename=" + URLEncoder.encode(tempFileName, "UTF-8"));
			out = response.getOutputStream();
			wb.write(out);			
			// 删除缓存模板文件
			FileOperate.deleteFile(tempFile);
		} catch (Exception e) {
			logger.error(e + "");
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		return Response.success(data);
	}	*/
    

	// 导出Excel	
	@RequestMapping(value = "/sampleItem/writeExcel", method = RequestMethod.GET)
	//@Privileges(name = "SAMPLE-ITEMS-INPUT-SELECT", scope = { 1 })
	public Response writeExcel(@RequestParam(name = "status", defaultValue = "0") int status,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "receiveSampleId", required = false) String receiveSampleId,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "username", required = false) String username,
			@RequestParam(name = "method", required = false) String method,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			HttpServletResponse response) {
		
		Map<String, Object> filter = new HashMap<String, Object>();
		if (status != 5) {
			filter.put("status", status);
		}
		if (StringUtils.isBlank(order)) {
			order = "updated_at desc";
		}
		if (!StringUtils.isBlank(receiveSampleId)) {
			filter.put("receive_sample_id", receiveSampleId);
		}
		if (!StringUtils.isBlank(reportId)) {
			filter.put("report_id", reportId);
		}
		if (!StringUtils.isBlank(username)) {
			filter.put("test_user", username);
		}
		if (!StringUtils.isBlank(sampleName)) {
			filter.put("sample_name", sampleName);
		}
		if (!StringUtils.isBlank(name)) {
			filter.put("name", name);
		}
		if (!StringUtils.isBlank(method)) {
			filter.put("method", method);
		}
		DataInputStream in=null;
		OutputStream out=null;
		try {



			PageInfo<ReceiveSampleItem> items = receiveSampleService.writeExcel(pageNum, pageSize, order, filter);

			XSSFWorkbook workbook = new XSSFWorkbook();
			Date date = new Date();
			SimpleDateFormat formatter1 = new SimpleDateFormat("HH-mm-ss");
			SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			String dateString1 = formatter1.format(date); 
			String dateString2 = formatter2.format(date); 

			//FileOutputStream fos = new FileOutputStream(new File("C:/Users/Administrator/Desktop/"+dateString2+"检测项.xlsx"));
			FileOutputStream fos = new FileOutputStream(new File("/var/lib/excel/"+dateString2+"检测项.xlsx"));
			XSSFSheet sheet = workbook.createSheet(dateString1);
			int a=0;
			XSSFRow row = sheet.createRow(a);
			row.createCell(0).setCellValue("报告编号");
			row.createCell(1).setCellValue("接样单编号");
			row.createCell(2).setCellValue("样品名称");
			row.createCell(3).setCellValue("项目名称");
			row.createCell(4).setCellValue("检验方法");
			row.createCell(5).setCellValue("判断依据");
			row.createCell(6).setCellValue("标准值");
			row.createCell(7).setCellValue("单位");
			row.createCell(8).setCellValue("检出限");
			row.createCell(9).setCellValue("要求完成日期");
			row.createCell(10).setCellValue("检测室");
			for ( ReceiveSampleItem pages1 : items.getList()) {
				a=a+1;
				XSSFRow row1 = sheet.createRow(a);
				//a.report_id,a.receive_sample_id,a.sample_name,b.`name`,b.method,a.execute_standard,b.standard_value,
				//b.unit,b.detection_limit,a.finish_date,b.test_room
				row1.createCell(0).setCellValue(pages1.getSample().getReportId());

				if(pages1.getSample().getReceiveSampleId()!=null){
					row1.createCell(1).setCellValue(pages1.getSample().getReceiveSampleId());
				}
				if(pages1.getSample().getSampleName()!=null){
					row1.createCell(2).setCellValue(pages1.getSample().getSampleName());
				}
				if(pages1.getName()!=null){
					row1.createCell(3).setCellValue(pages1.getName());
				}
				if(pages1.getMethod()!=null){
					row1.createCell(4).setCellValue(pages1.getMethod());
				}
				if(pages1.getSample().getExecuteStandard()!=null){
					row1.createCell(5).setCellValue(pages1.getSample().getExecuteStandard());
				}
				if(pages1.getStandardValue()!=null){
					row1.createCell(6).setCellValue(pages1.getStandardValue());
				}
				if(pages1.getUnit()!=null){
					row1.createCell(7).setCellValue(pages1.getUnit());
				}
				if(pages1.getDetectionLimit()!=null){
					row1.createCell(8).setCellValue(pages1.getDetectionLimit());
				}
				if(pages1.getSample().getFinishDate()!=null){
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String dateString = formatter.format(pages1.getSample().getFinishDate()); 
					row1.createCell(9).setCellValue(dateString);
				}
				if(pages1.getTestRoom()!=null){
					row1.createCell(10).setCellValue(pages1.getTestRoom());
				}



			}
			workbook.write(fos);
			fos.close();

			response.reset();
			response.setContentType("application/octet-stream;charset=UTF-8");
			response.setHeader("Content-disposition",
					"attachment;filename=" + URLEncoder.encode(dateString2+"检测项" + ".xlsx", "UTF-8"));

			//输入流：本地文件路径
			//in = new DataInputStream(new FileInputStream(new File("C:/Users/Administrator/Desktop/"+dateString2+"检测项" + ".xlsx")));  
			in = new DataInputStream(	new FileInputStream(new File("/var/lib/excel/"+dateString2+"检测项" + ".xlsx")));  
			out = response.getOutputStream();
			//输出文件
			int bytes = 0;
			byte[] bufferOut = new byte[1024];  
			while ((bytes = in.read(bufferOut)) != -1) {  
				out.write(bufferOut, 0, bytes);  
			}					
			return Response.success("success");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new BizException(e.toString());
		}finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}



	}   
    
    
    
    
    
    
    
}
