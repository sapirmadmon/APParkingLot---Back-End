package acs.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import acs.logic.ElementServiceImplementation;
import acs.rest.boundaries.ElementBoundary;

@RestController
public class ElementController {
	public ElementServiceImplementation es = new ElementServiceImplementation();

	@RequestMapping(path = "/acs/elements/{managerDomain}/{managerEmail}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ElementBoundary CreateNewElement(@PathVariable("managerDomain") String managerDomain,
			@PathVariable("managerEmail") String managerEmail, @RequestBody ElementBoundary elementDetails) {
		return es.create(managerDomain, managerEmail, elementDetails);
//		elementDetails.setElementId(new ElementIdBoundary("avichai", 3083462));
//		return elementDetails;
	}

	@RequestMapping(path = "/acs/elements/{userDomain}/{userEmail}/{elementDomain}/{elementId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ElementBoundary RetreiveElement(@PathVariable("userDomain") String userDomain,
			@PathVariable("userEmail") String userEmail, @PathVariable("elementDomain") String elementDomain,
			@PathVariable("elementId") int elementId) {

		return es.getSpecificElement(userDomain, userEmail, elementDomain, String.valueOf(elementId));
//		return CreateNewElement("managerDomain", "managerEmail",
//				new ElementBoundary(new ElementIdBoundary(userDomain, elementId), TypeEnum.CRITICAL, "avichai", true,
//						new Date(0), new Location(4.5, 3.6), Collections.singletonMap("key", "value"),
//						Collections.singletonMap("created by", "user")));
	}

	@RequestMapping(path = "/acs/elements/{userDomain}/{userEmail}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ElementBoundary[] RetreiveElementArr(@PathVariable("userDomain") String userDomain,
			@PathVariable("userEmail") String userEmail) {
		return es.getAll(userDomain, userEmail).toArray(new ElementBoundary[0]);
//		return IntStream.range(0, 5) // Stream of Integer
//				.mapToObj(i -> CreateNewElement("managerDomain", "managerEmail",
//						new ElementBoundary(new ElementIdBoundary(userDomain, i), TypeEnum.CRITICAL, "avichai",
//								true, new Date(0), new Location(4.5, 3.6), Collections.singletonMap("key", "value"),
//								Collections.singletonMap("created by", "user")))) // Stream
//																					// of
//																					// ElementBoundary
//				.collect(Collectors.toList()) // List of ElementBoundry
//				.toArray(new ElementBoundary[0]);

	}

	@RequestMapping(path = "/acs/elements/{managerDomain}/{managerEmail}/{elementDomain}/{elementId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public void updateUserDetails(@PathVariable("managerDomain") String managerDomain,
			@PathVariable("managerEmail") String managerEmail, @PathVariable("elementDomain") String elementDomain,
			@PathVariable("elementId") String elementId, @RequestBody ElementBoundary update) {
		// TODO implement this method to update element details
	}
}
