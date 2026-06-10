package com.tcs.genai.prompt.service.Impl;

import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapter;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapterFactory;


/**
 * Retrieves the payload for a specified vendor by using the appropriate vendor adapter.
 * 
 * This method accepts a common payload and the vendor name as input. It retrieves the
 * corresponding vendor-specific adapter using the VendorPayloadAdapterFactory. The adapter
 * is then used to generate and return the vendor-specific payload in the form of a JSONArray.
 *
 * @param commonPayload The common JSON payload that contains shared data for the vendor.
 * @param vendorName The name of the vendor for which the payload is to be generated.
 * @return A JSONArray representing the vendor-specific payload.
 * @author 1949882
 */

public class VendorPayloadService {

	public VendorPayloadAdapter getVendorAdaptor(String vendorName){
		
		VendorPayloadAdapterFactory payLoadAdapterFactory =  new VendorPayloadAdapterFactory();
		return payLoadAdapterFactory.getAdapter(vendorName) ;
	}
	
}