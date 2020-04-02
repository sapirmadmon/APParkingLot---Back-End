package acs.logic;

import java.util.List;

import acs.rest.boundaries.ElementBoundary;
import acs.rest.boundaries.UserBoundary;

public interface ElementService {

	public ElementBoundary create(String managerDomain , String managerEmail, ElementBoundary element);
	public ElementBoundary update(String managerDomain , String managerEmail,String elementDomain ,String elementId,ElementBoundary update);
	public List<ElementBoundary> getAll(String userDomain,String userEmail);
	public ElementBoundary getSpecificElement(String userDomain,String userEmail,String elemantDomain ,String elementId);
	public void deleteAllElements(String adminDomain ,String adminEmail);
	
}
