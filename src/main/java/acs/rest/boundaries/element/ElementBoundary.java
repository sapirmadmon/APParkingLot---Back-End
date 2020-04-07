package acs.rest.boundaries.element;

import java.util.Date;
import java.util.Map;

import acs.data.TypeEnum;
import acs.rest.boundaries.user.UserIdBoundary;

/*
{
    "elementId": {
        "domain": "{managerDomain}",
        "id": "5303776d-87d8-4d84-b8c3-b1240787e2a8"
    },
    "type": "demoElement",
    "name": "Parking Lot",
    "active": true,
    "timeStamp": "1970-01-01",
    "createBy": null,
    "location": {
        "lat": 35.3256,
        "lng": 46.0234
    },
    "elementAttributes": {
        "test": "great test",
        "parking type": "CRITICAL"
    }
}
 */
public class ElementBoundary {
	private ElementIdBoundary elementId;
	private TypeEnum type;
	private String name;
	private Boolean active;
	private Date timeStamp;
	private Map<String, UserIdBoundary> createBy;
	private Map<String, Double> location;
	private Map<String, Object> elementAttributes;

	public ElementBoundary(ElementIdBoundary elementId, TypeEnum type, String name, Boolean active, Date timeStamp,
			Map<String, Double> location, Map<String, Object> elemntAttributes, Map<String, UserIdBoundary> createBy) {
		super();
		this.elementId = elementId;
		this.type = type;
		this.name = name;
		this.active = active;
		this.timeStamp = timeStamp;
		this.createBy = createBy;
		this.location = location;
		this.elementAttributes = elemntAttributes;
	}

	public ElementBoundary() {
	}

	public TypeEnum getType() {
		return type;
	}

	public void setType(TypeEnum type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Map<String, Object> getElemntAttributes() {
		return elementAttributes;
	}

	public void setElemntAttributes(Map<String, Object> elemntAttributes) {
		this.elementAttributes = elemntAttributes;
	}

	public ElementIdBoundary getElementId() {
		return elementId;
	}

	public void setElementId(ElementIdBoundary elementId) {
		this.elementId = elementId;
	}

	public Map<String, Double> getLocation() {
		return location;
	}

	public Map<String, UserIdBoundary> getCreateBy() {
		return createBy;
	}

	public void setCreateBy(Map<String, UserIdBoundary> createBy) {
		this.createBy = createBy;
	}

	public Map<String, Object> getElementAttributes() {
		return elementAttributes;
	}

	public void setElementAttributes(Map<String, Object> elementAttributes) {
		this.elementAttributes = elementAttributes;
	}

	public void setLocation(Map<String, Double> location) {
		this.location = location;
	}
}
