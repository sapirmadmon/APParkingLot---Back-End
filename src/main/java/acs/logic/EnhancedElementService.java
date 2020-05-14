package acs.logic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import acs.rest.boundaries.element.ElementBoundary;
import acs.rest.boundaries.element.ElementIdBoundary;
import acs.rest.boundaries.user.UserIdBoundary;

public interface EnhancedElementService extends ElementService {
	//

	public void bindExistingElementToAnExsitingChildElement(ElementIdBoundary originId, ElementIdBoundary responseId);

	public Set<ElementBoundary> getAllChildrenOfAnExsitingElement(String userDomain, String userEmail,
			String elementDomain, String elementId);

	public Collection<ElementBoundary> getAnArrayWithElementParent(String userDomain, String userEmail,
			String elementDomain, String elementId);

	public List<ElementBoundary> getAll(String userDomain, String userEmail, int size, int page);

	public Collection<ElementBoundary> searchByLocation(UserIdBoundary userIdBoundary, double lat, double lng,
			double distance, int size, int page);

}
