package com.tcs.ion.ml.ws.qsnauthorv2;
import java.sql.SQLException;

import com.tcs.ion.ml.ws.bean.TransactionRequestBean;
import com.tcs.ion.ml.ws.bean.TransactionResponseBean;
import com.tcs.ion.ml.ws.bean.WebServiceParams;

@SuppressWarnings("all")
public interface QuestionGenerationWSV2 {
	TransactionResponseBean questionGenerationWebServiceImplement(TransactionRequestBean requestBean, WebServiceParams wsParams) throws IllegalArgumentException, SQLException, Exception;
	
	
}


