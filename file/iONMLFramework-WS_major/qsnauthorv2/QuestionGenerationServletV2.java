package com.tcs.ion.ml.ws.qsnauthorv2;


import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.ion.ml.ws.bean.TransactionRequestBean;
import com.tcs.ion.ml.ws.bean.TransactionResponseBean;
import com.tcs.ion.ml.ws.bean.WebServiceParams;
import com.tcs.ion.ml.ws.service.MergediONMLServlet;
import com.tcs.ion.ml.ws.service.ServiceLocator;
import com.tcs.ion.ml.ws.service.ValidateAndPrepareData;
import com.tcs.ion.ml.ws.util.ExceptionLogUtility;
import com.tcs.ion.ml.ws.util.IncreaseCounterForMeteredUser;
import com.tcs.ion.ml.ws.util.ValidateAndParseData;
import com.tcs.ion.ml.ws.util.MLWSConstants;

@SuppressWarnings("all")
@WebServlet("/QuestionGenerationServlet/v2")
public class QuestionGenerationServletV2 extends HttpServlet implements Servlet {
	private static final long serialVersionUID = 1L;
	private final Log l = LogFactory.getLog(QuestionGenerationServletV2.class);
	final String className = this.getClass().getName();
	long counter=0;
	boolean checkcounter;
	private IncreaseCounterForMeteredUser increasecounterformetereduser=ServiceLocator.instance().getIncreaseCounterForMeteredUser();
	private ValidateAndParseData validateparsedata=ServiceLocator.instance().getValidateparsedata();
	private QuestionGenerationWSV2 questionGenerationWSV2=ServiceLocator.instance().getQuestionGenerationWSV2();
	private ObjectMapper mapper=ServiceLocator.instance().getMapper();
	private ValidateAndPrepareData validateprepdata=ServiceLocator.instance().getValidateprepdata();
	public QuestionGenerationServletV2() {
		super();
	}

	//@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
//		if(! ("application/x-www-form-urlencoded".equals(request.getContentType())) ) {
//			return;
//		}
		
		l.error("Inside QuestionGenerationServletV2");

		Timestamp requestReceivedDate = new Timestamp(System.currentTimeMillis());
		TransactionRequestBean requestBean = null;
		TransactionResponseBean responseBean = new TransactionResponseBean();
		try {
			final WebServiceParams wsParams = this.validateprepdata.validateAndPrepareData(request,response,className);

			wsParams.setRequestReceivedDate(requestReceivedDate);
        	if((wsParams.getResponseCode()==0) || (wsParams.getResponseCode()==1) || (wsParams.getResponseCode()==2) ||(wsParams.getResponseCode()==111)) {
	        	l.info("-------responseMessage: {}"+wsParams.getResponseMessage());
	        	l.info("-------responseCode: {}"+wsParams.getResponseCode());
	        	responseBean.setResponseMessage(wsParams.getResponseMessage());
	        	responseBean.setResponseCode(wsParams.getResponseCode());
        	}
			else{
				if(wsParams.getMeteredOrNormalUser().equalsIgnoreCase(MLWSConstants.METERED_USER) || 
					wsParams.getMeteredOrNormalUser().equalsIgnoreCase(MLWSConstants.LIMIT_CROSSED) ||
					wsParams.getMeteredOrNormalUser().equalsIgnoreCase(MLWSConstants.LICENSE_END_DATE_CROSSED )||
					wsParams.getMeteredOrNormalUser().equalsIgnoreCase(MLWSConstants.LICENSE_START_DATE))
				{
					if(wsParams.isIncreaseCounter()){
						counter=wsParams.getApiCallCounter();
						l.error("Counter:"+counter);
						requestBean = this.validateparsedata.questionGenerationParseDataV2(request);
						if (requestBean == null) {
							l.error("Request Bean is null");
							responseBean.setResponseMessage(MLWSConstants.RESQUEST_STRING_REQUEST_TYPE_INVALID);
							responseBean.setResponseCode(999);
						}
						else {
							l.info("Request bean : {}"+requestBean.toString());
							responseBean = this.questionGenerationWSV2.questionGenerationWebServiceImplement(requestBean, wsParams);
							
							if (responseBean.getResponseMessage().equalsIgnoreCase(MLWSConstants.SUCCESS_MSG) &&
									responseBean.getResponseCode()==200)
							{
								l.info("Counter: {} "+counter+" AppID: {}"+wsParams.getAppId()+" OrgID: {} "+wsParams.getOrgId()+" BizId: {} "+wsParams.getBizCustId()+" Domain:{}"+wsParams.getDomain());
								checkcounter=increasecounterformetereduser.increaseCounter(counter,wsParams.getAppId(), wsParams.getOrgId(),wsParams.getBizCustId(),wsParams.getDomain());
								if(checkcounter==true){
									responseBean.setResponseMessage(MLWSConstants.SUCCESS_MSG);
									responseBean.setResponseCode(200);
								}
								else {
									responseBean.setResponseMessage(MLWSConstants.SUCESS_BUT_COUNTER_NOT_UPDATED);
									responseBean.setResponseCode(200);
								}
							}	
						}
					}
					else {
						responseBean.setResponseMessage(wsParams.getMeteredOrNormalUser());
						responseBean.setResponseCode(999);
					}
				}
				else {
					requestBean = this.validateparsedata.questionGenerationParseDataV2(request);
					if (requestBean == null) {
						responseBean.setResponseMessage(MLWSConstants.RESQUEST_STRING_REQUEST_TYPE_INVALID);
						responseBean.setResponseCode(999);
					}
					else {
						l.error("Request bean : {}"+requestBean);
						responseBean = this.questionGenerationWSV2.questionGenerationWebServiceImplement(requestBean, wsParams);
						l.info("Response received from Question Generation : {} " + responseBean);
					}	
				}
			}
		}
		catch (final Exception e) {
			ExceptionLogUtility.logException("Exception Occurred : {}", e, l);
		}
		finally  {
			responseBean.setDebugCode(MergediONMLServlet.serverIp);
			try {
				response.setContentType("application/json;charset=UTF-8");
				final String outputResult = this.mapper.writeValueAsString(responseBean);
				//l.error("Response {} :" + outputResult);
				response.getWriter().write(outputResult);		
			} 
			catch (final Exception e) {
				ExceptionLogUtility.logException("Exception Occurred in finally block: {}", e, l);
				JSONObject resultJson=new JSONObject();
				resultJson.put("Message", responseBean.getResponseMessage());
				response.getWriter().write(resultJson.toString());
			}
		}

	}

}
