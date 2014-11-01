package com.sunlights.account.biz;

import com.sunlights.account.vo.Capital4Product;
import com.sunlights.account.vo.TotalCapitalInfo;

import java.util.List;

/**
 * 
 * @author tangweiqun 2014/10/22
 *
 */
public interface CapitalService {
	
	public TotalCapitalInfo getTotalCapital(String custId, boolean takeCapital4Prd);
	
	public List<Capital4Product> getAllCapital4Product(String custId);
	
}
