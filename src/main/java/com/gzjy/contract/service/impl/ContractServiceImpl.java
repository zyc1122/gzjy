package com.gzjy.contract.service.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.ISelect;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gzjy.common.exception.BizException;
import com.gzjy.common.util.UUID;
import com.gzjy.common.util.fs.EpicNFSClient;
import com.gzjy.common.util.fs.EpicNFSService;
import com.gzjy.contract.mapper.ContractCommentMapper;
import com.gzjy.contract.mapper.ContractMapper;
import com.gzjy.contract.mapper.SampleMapper;
import com.gzjy.contract.model.Contract;
import com.gzjy.contract.model.ContractComment;
import com.gzjy.contract.model.ContractSample;
import com.gzjy.contract.model.ContractStatus;
import com.gzjy.contract.model.ContractTask;
import com.gzjy.contract.model.Sample;
import com.gzjy.contract.service.ContractService;
import com.gzjy.receive.service.ReceiveSampleService;
import com.gzjy.user.UserService;
import com.gzjy.user.model.User;

@Service
public class ContractServiceImpl implements ContractService {

	@Autowired
	private ContractMapper contractMapper;

	@Autowired
	private ContractCommentMapper contractCommentMapper;

	@Autowired
	private ProcessEngine processEngine;

	@Autowired
	private UserService userService;

	@Autowired
	private EpicNFSService epicNFSService;
	
	@Autowired
	private SampleMapper sampleMapper;

	private static Logger logger = LoggerFactory.getLogger(ReceiveSampleService.class);

	public Contract selectByPrimaryKey(String id) {
		return contractMapper.selectByPrimaryKey(id);
	}

	public int insert(Contract record) {
		return contractMapper.insertSelective(record);
	}

	public int deleteByPrimaryKey(String id) {
		return contractMapper.deleteByPrimaryKey(id);
	}

	public int updateByPrimaryKey(Contract record) {
		return contractMapper.updateByPrimaryKeySelective(record);
	}

	/**
	 * 部署合同审批流程实例 部署完成之后将流程ID反填到合同表中
	 * 
	 * @param approveUsers
	 *            合同审批的多个审批人
	 * @param updateContractUser
	 *            修改合同的人
	 * @return
	 */
	public void deploymentProcess(String contractId, ArrayList<String> approveUsers, String updateContractUser) {
		RuntimeService runtimeService = processEngine.getRuntimeService();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("approve_users", approveUsers);
		variables.put("resultCount", 0);
		variables.put("result", 1);
		variables.put("update_contract_user", updateContractUser);
		variables.put("taskComplete", new TaskComplete());
		String processId = runtimeService.startProcessInstanceByKey("ContractProcess", variables).getId();
		// 流程启动成功之后将返回的流程ID回填到合同contract表中
		Contract contract = new Contract();
		contract.setId(contractId);
		contract.setProcessId(processId);
		// 修改合同状态为审批中状态
		contract.setStatus(ContractStatus.APPROVING.getValue());
		contractMapper.updateByPrimaryKeySelective(contract);
	}

	/**
	 * 根据用户ID获取当前用户在合同流程中的任务
	 */
	public List<Task> getTaskByUserId() {
		String userId = userService.getCurrentUser().getId();
		TaskService taskService = processEngine.getTaskService();
		List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).list();
		return tasks;
	}

	/**
	 * 根据用户ID获取当前用户在合同流程中的历史任务
	 */
	public List<HistoricTaskInstance> getHistoryTaskByUserId() {
		String userId = userService.getCurrentUser().getId();
		HistoryService historyService = processEngine.getHistoryService();
		List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().finished()
				.taskAssignee(userId).list();
		logger.info("历史任务:" + tasks.size() + " userId:" + userId);
		return tasks;
	}

	public Task getUpdateTaskByProcessId(String processId) {
		TaskService taskService = processEngine.getTaskService();
		List<Task> tasks = taskService.createTaskQuery().executionId(processId).taskName("修改合同").list();
		return tasks.get(0);
	}

	/**
	 * 执行合同流程中的多人审批任务任务
	 * 
	 * @param task
	 * @param approve
	 */
	public void completeApproveTask(String taskId, String contractId, String approve, String context) {
		TaskService taskService = processEngine.getTaskService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		Task task = (Task) taskService.createTaskQuery().taskId(taskId).list().get(0);
		// 如果是审批流程中的同意状态，则需要将result值加1，否则不需额外操作
		if (approve.equals("true")) {
			int result = (int) runtimeService.getVariable(task.getExecutionId(), "resultCount");
			runtimeService.setVariable(task.getExecutionId(), "resultCount", result + 1);
		}
		taskService.complete(taskId);
		// 将评审意见插入到合同评审意见表中
		User currentUser = userService.getCurrentUser();
		ContractComment contractComment = new ContractComment();
		contractComment.setId(UUID.random());
		contractComment.setTaskId(taskId);
		contractComment.setUserId(currentUser.getId());
		contractComment.setUserName(currentUser.getName());
		contractComment.setContext(context);
		contractComment.setContractId(contractId);
		contractComment.setCreateTime(new Date());
		contractCommentMapper.insertSelective(contractComment);
	}

	/**
	 * 执行合同流程中的修改合同任务
	 * @param task
	 * @param approve
	 */

	public void completeUpdateTask(String taskId) {
		RuntimeService runtimeService = processEngine.getRuntimeService();
		TaskService taskService = processEngine.getTaskService();
		Task task = (Task) taskService.createTaskQuery().taskId(taskId).list().get(0);
		runtimeService.setVariable(task.getExecutionId(), "resultCount", 0);
		taskService.complete(taskId);
	}

	public PageInfo<Contract> getPageList(Integer pageNum, Integer pageSize, String detectProject, String type, List<Integer> status) {
		List<Contract> list = new ArrayList<Contract>();
		PageInfo<Contract> pages = new PageInfo<Contract>(list);
		pages = PageHelper.startPage(pageNum, pageSize).doSelectPageInfo(new ISelect() {
			@Override
			public void doSelect() {
				contractMapper.selectAll(detectProject, type, status);
			}
		});
		return pages;
	}

	public void updateStatusByProcessId(Integer status, String processId) {
		contractMapper.updateStatusByProcessId(status, processId);
	}

	public String checkContractProtocolId(String ProtocolId) {
		return contractMapper.checkContractProtocolId(ProtocolId);
	}

	public void uploadFile(MultipartFile[] files, String catalog) {
		EpicNFSClient client = epicNFSService.getClient("gzjy");
		String dir = "attachment\\"+catalog;
		if (!client.hasRemoteDir(dir)) {
			client.createRemoteDir(dir);
		}
		if (files != null && files.length > 0) {
			try {
				for (int i = 0; i < files.length; i++) {
					MultipartFile file = files[i];
					client.upload(file.getInputStream(), dir+"\\" + file.getOriginalFilename());
				}
				client.close();
			} catch (Exception e) {
				logger.info("文件上传失败:" + e);
				throw new BizException("文件上传失败");
			}
		}
	}

	public String getMaxIdByType(String type) {
		return contractMapper.getMaxIdByType(type);
	}

	public String generateContractId(String contractType, String foodType) {
		SimpleDateFormat  df = new SimpleDateFormat ("yyyyMM");
		String ymd = df.format(new Date());
		String contractId = "";		
		//是否是三品一标合同
		if(!contractType.equals("pb")){			
			//如果是食品合同
			if(foodType.equals("true")){
				contractId +="F";
			}else {
				contractId +="Q";
			}
			//政府
			if(contractType.equals("government")){	
				contractId +="S";
			}else {
				contractId +="Y";
			}
		}else {
			contractId +="FN";
		}
		contractId +=ymd;
		String str ="0000";
		String maxId = contractMapper.getMaxIdByType(contractType);	
		if(maxId == null || maxId.equals("")) {
		    return contractId+"0001";
		}
		int maxIdLength = maxId.length();
		int data = Integer.parseInt(maxId.substring(maxIdLength-4, maxIdLength));		
		data+=1;		
		String str_m=str.substring(0, 4-(data+"").length())+data;
		return contractId+str_m;
	}

	public ContractSample getContractDetail(String contractId) {
		Contract contract = contractMapper.selectByPrimaryKey(contractId);
		List<Sample> sampleList = sampleMapper.getListByContractId(contractId);
		ContractSample contractSample = new ContractSample();
		contractSample.setContract(contract);
		contractSample.setSampleList(sampleList);
		return contractSample;
	}
	
	public OutputStream getAppendix(OutputStream out, String contractId, String filename) {
		EpicNFSClient client = epicNFSService.getClient("gzjy");
		String filePath = "attachment/"+contractId+"/"+filename;
		if(!client.hasRemoteFile(filePath)) {
			return null;
		}
		try {
			InputStream inputStream = new FileInputStream("/var/lib/docs/gzjy/"+filePath);        
	        byte[] b = new byte[1024];
	        int i=0;
	        while((i=inputStream.read(b))!=-1){
	            out.write(b, 0, i);
	        }
	        inputStream.close();
	        out.close();
		}
		catch (Exception e) {
			logger.info(e.getMessage());
		}
		return out;
	}

	
	public void deleteAppendix(String contractId, String filename) {
		EpicNFSClient client = epicNFSService.getClient("gzjy");
		String filePath = "attachment\\"+contractId+"\\"+filename;
		if(client.hasRemoteFile(filePath)) {
			client.deleteRemoteFile(filePath);
		}
	}

	public String getAppendixById(String id) {
		return contractMapper.getAppendixById(id);
	}
	
	/**
	 * 根据processId用户任务(已完成/未完成)
	 * @return ArrayList<ContractTask>
	 */	
	public ArrayList<ContractTask> getAllContractTaskByUserName(String processId, String isHandle) {
		String userId = userService.getCurrentUser().getId();
		ArrayList<ContractTask> taskList= new ArrayList<ContractTask>();
		if(isHandle.equals("0")) {
			TaskService taskService = processEngine.getTaskService();
			List<Task> tasksReady = taskService.createTaskQuery().taskAssignee(userId).processDefinitionId(processId).list();
			for (Task task : tasksReady) {
				ContractTask contractTask = new ContractTask();
				contractTask.setId(task.getId());
				contractTask.setName(task.getName());
				contractTask.setAssignee(task.getAssignee());
				contractTask.setCreateTime(task.getCreateTime());
				contractTask.setProcessInstanceId(task.getProcessInstanceId());
				contractTask.setExecutionId(task.getExecutionId());
				taskList.add(contractTask);
			}		
		}
		else {
			HistoryService historyService = processEngine.getHistoryService();
			List<HistoricTaskInstance> tasksComplete = historyService.createHistoricTaskInstanceQuery().finished()
						.taskAssignee(userId).processDefinitionId(processId).list();			
			for (HistoricTaskInstance task : tasksComplete) {				
				ContractTask contractTask = new ContractTask();
				contractTask.setId(task.getId());
				contractTask.setName(task.getName());
				contractTask.setAssignee(task.getAssignee());
				contractTask.setCreateTime(task.getCreateTime());
				contractTask.setProcessInstanceId(task.getProcessInstanceId());
				contractTask.setExecutionId(task.getExecutionId());
				taskList.add(contractTask);
			}
		}
		return taskList;
	}
}
