package com.gzjy.receive.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.activiti.engine.task.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.pagehelper.PageInfo;
import com.gzjy.common.Response;
import com.gzjy.common.ShortUUID;
import com.gzjy.common.annotation.Privileges;
import com.gzjy.common.exception.BizException;
import com.gzjy.common.util.fs.EpicNFSService;
import com.gzjy.receive.mapper.ReportExtendMapper;
import com.gzjy.receive.model.ReceiveSample;
import com.gzjy.receive.model.ReceiveSampleItem;
import com.gzjy.receive.model.ReceiveSampleTask;
import com.gzjy.receive.model.ReportExtend;
import com.gzjy.receive.service.ReceiveSampleService;
import com.gzjy.receive.service.ReportService;
import com.gzjy.template.mapper.TemplateMapper;
import com.gzjy.user.UserService;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

@RestController
@RequestMapping(value = "v1/ahgz/report")

public class ReportController {
	private static Logger logger = LoggerFactory.getLogger(ReportController.class);

	@Autowired
	private ReportService reportService;
	@Autowired
	UserService userService;
	@Autowired
	ReceiveSampleService receiveSampleService;
	@Autowired
	ReportExtendMapper reportExtendMapper;
	@Autowired
	TemplateMapper templateMapper;
	@Autowired
	private DataSource dataSource;
	@Autowired
	private EpicNFSService epicNFSService;
	@RequestMapping(value = "/edit", method = RequestMethod.POST)
	@Privileges(name = "REPORT-EDIT", scope = { 1 })
	public Response editReport(@RequestBody(required = true) ReceiveSample receiveSample) throws Exception {
		try {
			reportService.editReceiveSample(receiveSample);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 根据reportId获取报告详情
	 * 
	 * @param reportId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{reportId}", method = RequestMethod.GET)
	@Privileges(name = "REPORT-DETAIL", scope = { 1 })
	public Response reportDetail(@PathVariable(required = true) String reportId) throws Exception {
		try {
			ReceiveSample receiveSample = reportService.getReceiveSample(reportId);
			ReportExtend reportExtend = reportExtendMapper.selectByReportId(receiveSample.getReportId());
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("receiveSample", receiveSample);
			result.put("reportExtend", reportExtend);
			return Response.success(result);
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 启动报告流程引擎接口
	 * 
	 * @param receiveSampleId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/process", method = RequestMethod.POST)
	@Privileges(name = "REPORT-PROCESS-START", scope = { 1 })
	public Response startProcess(@RequestParam(required = true) String reportId) throws Exception {
		try {
			reportService.deploymentProcess(reportId);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 查看当前用户任务列表
	 * @param isHandle(0表示查看未完成任务，1     表示查看已完成任务)
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/process", method = RequestMethod.GET)
	//@Privileges(name = "REPORT-TASK-USER-GET", scope = { 1 })
	public Response getTaskByUser(@RequestParam(required = true, defaultValue = "0") String isHandle) throws Exception {
		try {
			ArrayList<ReceiveSampleTask> result = reportService.getReportTaskByUserName(isHandle);
			return Response.success(result);
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 根据任务编号获取评论意见
	 * 
	 * @param taskId
	 * @return List<Comment>
	 * @throws Exception
	 */	
	@RequestMapping(value = "/process/task/{taskId}/comments", method = RequestMethod.GET)
	public Response getCommentsByTaskId(@PathVariable String taskId) throws Exception {
		try {
			List<Comment> result = reportService.getCommentByTaskId(taskId);
			return Response.success(result);
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 根据流程引擎编号processId获取评论意见
	 * 
	 * @param processId
	 * @return List<Comment>
	 * @throws Exception
	 */
	//@Privileges(name = "REPORT-TASK-COMMENT-GET", scope = { 1 })
	@RequestMapping(value = "/process/task/{processId}/comments/all", method = RequestMethod.GET)
	public Response getCommentsByProcessId(@PathVariable String processId) throws Exception {
		try {
			List<Comment> result = reportService.getCommentByProcessId(processId);
			return Response.success(result);
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 执行合同流程中编辑任务
	 * @param taskId             任务编号
	 * @param receiveSampleId    收样单编号
	 * @param examinePersonId    审核人编号
	 * @param comment            评论意见
	 * @return
	 */
	@RequestMapping(value = "/process/task/edit/{taskId}", method = RequestMethod.GET)
	@Privileges(name = "REPORT-TASK-EDIT-DO", scope = { 1 })
	public Response completeEditTask(@PathVariable String taskId, @RequestParam(required = true) String reportId,
			@RequestParam(required = true) String examinePersonName, @RequestParam(required = false) String comment,
			@RequestParam(required = true) String reportProcessId) {
		try {
			reportService.doEditTask(taskId, reportId, examinePersonName, comment, reportProcessId);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 执行合同流程中审核任务
	 * @param taskId            任务编号
	 * @param receiveSampleId   收样单编号
	 * @param approvePersonId   批准人编号
	 * @param pass              是否通过
	 * @param comment           评论意见
	 * @return
	 */
	@RequestMapping(value = "/process/task/examine/{taskId}", method = RequestMethod.GET)
	@Privileges(name = "REPORT-TASK-EXAMINE-DO", scope = { 1 })
	public Response completeExamineTask(@PathVariable String taskId,
			@RequestParam(required = true) String reportId,
			@RequestParam(required = false) String approvePersonName, @RequestParam(required = true) boolean pass,
			@RequestParam(required = false) String comment, @RequestParam(required = true) String reportProcessId) {
		try {
			reportService.doExamineTask(taskId, reportId, approvePersonName, pass, comment, reportProcessId);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 执行合同流程中批准任务
	 * @param taskId            任务编号
	 * @param receiveSampleId   收样单编号
	 * @param pass              是否通过
	 * @param comment           评论意见
	 * @return
	 */
	@Privileges(name = "REPORT-TASK-APPROVE-DO", scope = { 1 })
	@RequestMapping(value = "/process/task/approve/{taskId}", method = RequestMethod.GET)
	public Response completeApproveTask(@PathVariable String taskId,
			@RequestParam(required = true) String reportId, @RequestParam(required = true) boolean pass,
			@RequestParam(required = false) String comment, @RequestParam(required = true) String reportProcessId) {
		try {
			reportService.doApproveTask(taskId, reportId, pass, comment, reportProcessId);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 查看待编辑报告列表
	 */
	@RequestMapping(value = "/list/edit", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-EDIT-GET", scope = { 1 })
	public Response getEditListReports(@RequestParam(name = "receiveSampleId", required = false) String id,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			// @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "isHandle", required = false, defaultValue="0") String isHandle) {

		PageInfo<ReceiveSample> reports = reportService.getReportByCondition(id, reportId, entrustedUnit, inspectedUnit,
				sampleName, executeStandard, productionUnit, sampleType, checkType, 0, order, 1, pageNum, pageSize,
				startTime, endTime, "draw_user",isHandle);
		List<Object> data = new ArrayList<Object>();
		for (ReceiveSample receiveSample : reports.getList()) {
			HashMap<String, Object> result = new HashMap<String, Object>();
			ArrayList<ReceiveSampleTask> task = reportService
					.getAllReportTaskByUserName(receiveSample.getReportProcessId(), isHandle);
			result.put("report", receiveSample);
			result.put("task", task);			
			data.add(result);
		}
		Map<String,Object> info = new HashMap<String,Object>();
		info.put("total", reports.getTotal());
		info.put("pages", reports.getPages());
		info.put("list", data);
		return Response.success(info);
	}

	/**
	 * 查看待审核报告列表
	 */
	@RequestMapping(value = "/list/examine", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-EXAMINE-GET", scope = { 1 })
	public Response getExamineListReports(@RequestParam(name = "receiveSampleId", required = false) String id,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			// @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "isHandle", required = false, defaultValue="0") String isHandle) {

		PageInfo<ReceiveSample> reports = reportService.getReportByCondition(id, reportId, entrustedUnit, inspectedUnit,
				sampleName, executeStandard, productionUnit, sampleType, checkType, 1, order, 1, pageNum, pageSize,
				startTime, endTime, "examine_user",isHandle);
		List<Object> data = new ArrayList<Object>();
		for (ReceiveSample receiveSample : reports.getList()) {
			HashMap<String, Object> result = new HashMap<String, Object>();
			ArrayList<ReceiveSampleTask> task = reportService
					.getAllReportTaskByUserName(receiveSample.getReportProcessId(),isHandle);
			result.put("report", receiveSample);
			result.put("task", task);			
			data.add(result);
		}
		Map<String,Object> info = new HashMap<String,Object>();
		info.put("total", reports.getTotal());
		info.put("pages", reports.getPages());
		info.put("list", data);
		return Response.success(info);
	}

	/**
	 * 查看待批准报告列表
	 */
	@RequestMapping(value = "/list/approve", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-APPROVE-GET", scope = { 1 })
	public Response getApproveListReports(@RequestParam(name = "receiveSampleId", required = false) String id,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			// @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "isHandle", required = false, defaultValue="0") String isHandle) {

		PageInfo<ReceiveSample> reports = reportService.getReportByCondition(id, reportId, entrustedUnit, inspectedUnit,
				sampleName, executeStandard, productionUnit, sampleType, checkType, 2, order, 1, pageNum, pageSize,
				startTime, endTime, "approval_user",isHandle);
		List<Object> data = new ArrayList<Object>();
		for (ReceiveSample receiveSample : reports.getList()) {
			HashMap<String, Object> result = new HashMap<String, Object>();
			ArrayList<ReceiveSampleTask> task = reportService
					.getAllReportTaskByUserName(receiveSample.getReportProcessId(), isHandle);
			result.put("report", receiveSample);
			result.put("task", task);			
			data.add(result);
		}
		Map<String,Object> info = new HashMap<String,Object>();
		info.put("total", reports.getTotal());
		info.put("pages", reports.getPages());
		info.put("list", data);
		return Response.success(info);
	}

	/**
	 * 查看待打印报告列表
	 */
	@RequestMapping(value = "/list/print", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-PRINT-GET", scope = { 1 })
	public Response getPrintListReports(@RequestParam(name = "receiveSampleId", required = false) String id,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			// @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "isHandle", required = false, defaultValue="0") String isHandle) {

		PageInfo<ReceiveSample> result = reportService.getReportByCondition(id, reportId, entrustedUnit, inspectedUnit,
				sampleName, executeStandard, productionUnit, sampleType, checkType, 3, order, status, pageNum, pageSize,
				startTime, endTime, "",isHandle);
		return Response.success(result);
	}

	/**
	 * 查看已完成报告列表
	 */
	@RequestMapping(value = "/list/finish", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-FINISH-GET", scope = { 1 })
	public Response getFinishListReports(@RequestParam(name = "receiveSampleId", required = false) String id,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			// @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime) {

		PageInfo<ReceiveSample> result = reportService.getReportByCondition(id, reportId, entrustedUnit, inspectedUnit,
				sampleName, executeStandard, productionUnit, sampleType, checkType, 4, order, 1, pageNum, pageSize,
				startTime, endTime, "","0");
		return Response.success(result);
	}

	/**
	 * 查看所有报告列表
	 */
	@RequestMapping(value = "/list/all", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-ALL-GET", scope = { 1 })
	public Response getAllListReports(@RequestParam(name = "receiveSampleId", required = false) String id,
			@RequestParam(name = "reportId", required = false) String reportId,
			@RequestParam(name = "entrustedUnit", required = false) String entrustedUnit,
			@RequestParam(name = "inspectedUnit", required = false) String inspectedUnit,
			@RequestParam(name = "sampleName", required = false) String sampleName,
			@RequestParam(name = "executeStandard", required = false) String executeStandard,
			@RequestParam(name = "productionUnit", required = false) String productionUnit,
			@RequestParam(name = "sampleType", required = false) String sampleType,
			@RequestParam(name = "checkType", required = false) String checkType,
			// @RequestParam(name = "reportStatus", required = false) Integer reportStatus,
			@RequestParam(name = "order", required = false) String order,
			@RequestParam(name = "status", defaultValue = "5") int status,
			@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime) {

		PageInfo<ReceiveSample> result = reportService.getReportByCondition(id, reportId, entrustedUnit, inspectedUnit,
				sampleName, executeStandard, productionUnit, sampleType, checkType, null, order, 1, pageNum,
				pageSize, startTime, endTime, "","0");
		return Response.success(result);
	}

	/**
	 * 查看报告中的检验项
	 * @return
	 */
	@RequestMapping(value = "/list/{reportId}", method = RequestMethod.GET)
	@Privileges(name = "REPORT-LIST-ITEM-GET", scope = { 1 })
	public Response detailReportItem(@PathVariable(name = "reportId", required = true) String reportId) {
		try {
			List<ReceiveSampleItem> getReceiveSampleItemList = reportService.getReceiveSampleItemList(reportId);
			return Response.success(getReceiveSampleItemList);
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 修改报告中的检验项
	 * 
	 * @return
	 */
	@RequestMapping(value = "/item/{reportId}", method = RequestMethod.POST)
	@Privileges(name = "REPORT-LIST-ITEM-UPDATE", scope = { 1 })
	public Response detailReport(@RequestBody List<ReceiveSampleItem> items) {

		try {
			for (ReceiveSampleItem record : items) {
				reportService.updateReceiveSampleItem(record);
			}
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 修改报告中的签发日期
	 * 
	 * @return
	 */
	@RequestMapping(value = "/{reportId}/property/qianfa", method = RequestMethod.PUT)
	//@Privileges(name = "REPORT-QIANFA-UPDATE", scope = { 1 })
	public Response updateQianfa(
			@PathVariable(name = "reportId", required = true) String reportId,
			@RequestParam(required = true) String qianfaDate) {
		ReceiveSample record = new ReceiveSample();
		record.setReportId(reportId);
		SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			record.setQianfaDate(myFmt2.parse(qianfaDate));
			reportService.updateReceiveSample(record);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 指定报告对应魔板
	 * 
	 * @return
	 */
	@RequestMapping(value = "/template/bind", method = RequestMethod.POST)
	//@Privileges(name = "REPORT-TEMPLATE-BIND", scope = { 1 })
	public Response receiveBindTemplate(@RequestBody ReportExtend reportExtend) {
		reportExtend.setId(ShortUUID.getInstance().generateShortID());
		try {
			reportService.insertReportExtend(reportExtend);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 修改报告对应魔板
	 * 
	 * @return
	 */
	@RequestMapping(value = "/template/bind/{id}", method = RequestMethod.PUT)
	//@Privileges(name = "REPORT-TEMPLATE-UPDATE", scope = { 1 })
	public Response receiveBindTemplateEdit(@PathVariable(name = "id", required = true) String id,
			@RequestParam(name = "reportId", required = true) String reportId,
			@RequestParam(name = "templateId", required = true) String templateId,
			@RequestParam(name = "templateName", required = true) String templateName,
			@RequestParam(name = "templateDesc", required = false) String templateDesc) {

		ReportExtend record = new ReportExtend();
		record.setId(id);
		record.setReportId(reportId);
		record.setTemplateId(templateId);
		record.setTemplateName(templateName);
		record.setTemplateDesc(templateDesc);
		try {
			reportService.updateReportExtend(record);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 删除报告对应魔板
	 * 
	 * @return
	 */
	@RequestMapping(value = "/template/bind/{id}", method = RequestMethod.DELETE)
	//@Privileges(name = "REPORT-TEMPLATE-DELETE", scope = { 1 })
	public Response receiveBindTemplateEdit(@PathVariable(name = "id", required = true) String id) {
		try {
			reportService.deleteReportExtend(id);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

	/**
	 * 报告的导出pdf
	 * 
	 * @return
	 */
	@RequestMapping(value = "/export", method = RequestMethod.GET)
	//@Privileges(name = "REPORT-PDF-EXPORT", scope = { 1 })
	public Response exportReport(@RequestParam(name = "reportId", required = true) String reportId,
			HttpServletResponse response) {
		OutputStream out = null;
		try {
			ReceiveSample result = receiveSampleService.getReceiveSample(reportId);
			if (result == null) {
				return Response.fail("未查询到对应的报告信息");
			}
			ReportExtend reportExtend = reportExtendMapper.selectByReportId(result.getReportId());
			if (reportExtend == null) {
				throw new BizException("报告模板不存在");
			}
			String templateName = templateMapper.selectById(reportExtend.getTemplateId()).getExcelName();

			String templateDir = "/var/lib/docs/gzjy/template/" + templateName;
			// 设定报表所需要的外部参数内容
			Map<String, Object> rptParameters = new HashMap<String, Object>();
			if(templateName.contains("green")) {
			    int count=receiveSampleService.getCountsByReportId(reportId);
			    rptParameters.put("items_count", count);
			}
			
			rptParameters.put("reportId", reportId);
			// 传入报表源文件绝对路径，外部参数对象，DB连接，得到JasperPring对象
			JasperPrint jasperPrint = JasperFillManager.fillReport(templateDir, rptParameters,
					dataSource.getConnection());
			response.reset();
			response.setContentType("application/octet-stream;charset=UTF-8");
			// response.setDateHeader("Expires", 0); // 清除页面缓存
			response.setHeader("Content-disposition",
					"attachment;filename=" + URLEncoder.encode(reportId + ".pdf", "UTF-8"));
			out = response.getOutputStream();
			JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			out.flush();
			logger.info("Export success!!");
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
                if(!dataSource.getConnection().isClosed()) {
                    dataSource.getConnection().close();
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}
	}

	/**
	 * 预览报告
	 * 
	 * @return
	 */
	@RequestMapping(value = "/preview", method = RequestMethod.GET)
	//@Privileges(name = "REPORT-PREVIEW", scope = { 1 })
	public Response viewReport(@RequestParam(name = "reportId", required = true) String reportId,
			@RequestParam(name = "type", required = true) String type, HttpServletResponse response) {
		try {
			ReceiveSample result = receiveSampleService.getReceiveSample(reportId);
			if (result == null) {
				return Response.fail("未查询到对应的报告信息");
			}
			ReportExtend reportExtend = reportExtendMapper.selectByReportId(result.getReportId());

			if (reportExtend == null) {
				throw new BizException("报告模板不存在");
			}
			String templateName = templateMapper.selectById(reportExtend.getTemplateId()).getExcelName();
			String templateDir = "/var/lib/docs/gzjy/template/" + templateName;

			// 设定报表所需要的外部参数内容
			Map<String, Object> rptParameters = new HashMap<String, Object>();
			if(templateName.contains("green")) {
                int count=receiveSampleService.getCountsByReportId(reportId);
                rptParameters.put("items_count", count);
            }
			rptParameters.put("reportId", reportId);
			// 传入报表源文件绝对路径，外部参数对象，DB连接，得到JasperPring对象
			JasperPrint jasperPrint = JasperFillManager.fillReport(templateDir, rptParameters,
					dataSource.getConnection());
			response.reset();

			OutputStream out = response.getOutputStream();
			if (type.equals("xml")) {
				response.setContentType("text/html;charset=UTF-8");
				// response.setDateHeader("Expires", 0); // 清除页面缓存
				response.setHeader("Content-disposition",
						"inline;filename=" + URLEncoder.encode(reportId + ".xml", "UTF-8"));
				JasperExportManager.exportReportToXmlStream(jasperPrint, out);
			} else if (type.equals("html")) {
				response.setContentType("text/html;charset=UTF-8");
				// response.setDateHeader("Expires", 0); // 清除页面缓存
				response.setHeader("Content-disposition",
						"inline;filename=" + URLEncoder.encode(reportId + ".html", "UTF-8"));
				HtmlExporter exporter = new HtmlExporter();
				exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
				exporter.setExporterOutput(new SimpleHtmlExporterOutput(out));

				exporter.exportReport();

			} else if (type.equals("pdf")) {
				response.setContentType("application/pdf;charset=UTF-8");
				// response.setDateHeader("Expires", 0); // 清除页面缓存
				response.setHeader("Content-disposition",
						"inline;filename=" + URLEncoder.encode(reportId + ".pdf", "UTF-8"));
				JasperExportManager.exportReportToPdfStream(jasperPrint, out);
			}

			out.flush();
			if (out != null) {
				out.close();
			}
			logger.info("Export success!!");
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		} finally {
		    try {
                if(!dataSource.getConnection().isClosed()) {
                    dataSource.getConnection().close();
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

		}
	}

	/**
	 * 批量预览pdf报告
	 * 
	 * @return
	 */
	@RequestMapping(value = "/preview", method = RequestMethod.POST)
	//@Privileges(name = "REPORT-MUTI-PREVIEW", scope = { 1 })
	public Response batchviewReport(@RequestBody() List<String> reportIds, HttpServletResponse response) {
		List<JasperPrint> prints = new ArrayList<JasperPrint>();
		OutputStream out = null;
		for (String id : reportIds) {
			ReceiveSample node = receiveSampleService.getReceiveSample(id);
			if (node == null) {
				logger.error("抽验单编号：" + id + "不存在");
			} else {
				ReportExtend reportExtend = reportExtendMapper.selectByReportId(node.getReportId());

				if (reportExtend == null) {
					logger.error("抽验单编号：" + id + "的模板不存在");
				} else {
					String templateName = templateMapper.selectById(reportExtend.getTemplateId()).getExcelName();
					String templateDir = "/var/lib/docs/gzjy/template/" + templateName;
					Map<String, Object> rptParameters = new HashMap<String, Object>();
					if(templateName.contains("green")) {
		                int count=receiveSampleService.getCountsByReportId(id);
		                rptParameters.put("items_count", count);
		            }
					rptParameters.put("reportId", id);
					// 传入报表源文件绝对路径，外部参数对象，DB连接，得到JasperPring对象
					JasperPrint jasperPrint = new JasperPrint();
					try {
						jasperPrint = JasperFillManager.fillReport(templateDir, rptParameters,
								dataSource.getConnection());
					} catch (Exception e) {
						e.printStackTrace();
						return Response.fail(e.getMessage());
					}
					prints.add(jasperPrint);
				}
			}
		}
		if (prints.size() == 0) {
			logger.error("没有打印的报告");
			return Response.fail("没有打印的报告");
		} else {

			try {
				response.reset();
				response.setContentType("application/pdf;charset=UTF-8");
				// response.setContentType("arraybuffer;charset=UTF-8");
				// response.setDateHeader("Expires", 0); // 清除页面缓存
				response.setHeader("Content-disposition",
						"inline;filename=" + URLEncoder.encode(UUID.randomUUID() + ".pdf", "UTF-8"));
				out = response.getOutputStream();
				JRPdfExporter exporter = new JRPdfExporter();
				exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
				exporter.exportReport();
				out.flush();

				logger.info("batchExport success!!");
				return Response.success("success");
			} catch (Exception e) {
				logger.error(e + "");
				return Response.fail(e.getMessage());
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
                    if(!dataSource.getConnection().isClosed()) {
                        dataSource.getConnection().close();
                    }
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
			}

		}
	}

	/**
	 * 批量修改报告reportStatus
	 * 
	 * @return
	 */
	@RequestMapping(value = "/status/muti", method = RequestMethod.GET)
	//@Privileges(name = "REPORT-MUTI-STATUS-UPDATE", scope = { 1 })
	public Response viewReport(@RequestParam(name = "reportStatus", required = true) String reportStatus,
			@RequestParam(name = "reportIdList", required = true) String reportIdList) {
		String[] temp = reportIdList.split(";");
		List<String> idList = new ArrayList<String>();
		for(String s:temp) {
			idList.add(s);
		}
		try {
			reportService.mutiUpdateReportStatusByReportIdList(reportStatus, idList);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}
	
	/**
	 * 修改编制人接口
	 * @return
	 */
	@RequestMapping(value = "/drawuser", method = RequestMethod.PUT)
	//@Privileges(name = "REPORT-MUTI-STATUS-UPDATE", scope = { 1 })
	public Response modifyDrawUser(
			@RequestParam(required = true) String reportId,
			@RequestParam(required = true) String processId,
			@RequestParam(required = true) String newDrawUser) {
		try {
			reportService.modifyDrawUser(reportId, processId, newDrawUser);
			return Response.success("success");
		} catch (Exception e) {
			logger.error(e + "");
			return Response.fail(e.getMessage());
		}
	}

}
