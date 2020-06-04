package acs.logic.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.util.ArrayUtils;

import acs.dal.ActionDao;
import acs.dal.ElementDao;
import acs.dal.UserDao;
import acs.data.ActionEntity;
import acs.data.Converter;
import acs.data.ElementEntity;
import acs.data.ElementIdEntity;
import acs.data.UserEntity;
import acs.data.UserIdEntity;
import acs.data.UserRole;
import acs.logic.EnhancedActionService;
import acs.logic.EnhancedElementService;
import acs.logic.EnhancedUserService;
import acs.logic.ObjectNotFoundException;
import acs.logic.ServiceTools;
import acs.rest.boundaries.action.ActionBoundary;
import acs.rest.boundaries.action.ActionIdBoundary;
import acs.rest.boundaries.action.ActionType;
import acs.rest.boundaries.element.ElementBoundary;
import acs.rest.boundaries.element.ElementIdBoundary;
import acs.rest.boundaries.element.ElementType;
import acs.rest.boundaries.element.Location;
import acs.rest.boundaries.user.UserBoundary;

@Service
public class DbActionServiceImplementation implements EnhancedActionService {
	private String projectName;
	private ActionDao actionDao;
	private ElementDao elementDao;
	private UserDao userDao;
	private Converter converter;
	private EnhancedElementService elementService;
	private EnhancedUserService userService;

	@Autowired
	public DbActionServiceImplementation(ActionDao actionDao, ElementDao elementDao, UserDao userDao,
			Converter converter, EnhancedUserService userService, EnhancedElementService elementService) {
		this.converter = converter;
		this.actionDao = actionDao;
		this.userDao = userDao;
		this.elementDao = elementDao;
		this.elementService = elementService;
		this.userService = userService;
	}

	// injection of project name from the spring boot configuration
	@Value("${spring.application.name: generic}")
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@Override
	@Transactional // (readOnly = false)
	public Object invokeAction(ActionBoundary action) {

		if (action == null || action.getType() == null)
			throw new RuntimeException("ActionBoundary received in invokeAction method can't be null\n");

		UserBoundary userBoundary = this.userService.login(action.getInvokedBy().getUserId().getDomain(),
				action.getInvokedBy().getUserId().getEmail());

		if (!userBoundary.getRole().equals(UserRole.PLAYER))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "only player can invoke action");
		System.out.println("testitbefore!!");
		ElementBoundary element = this.elementService.getSpecificElement(action.getInvokedBy().getUserId().getDomain(),
				action.getInvokedBy().getUserId().getEmail(), action.getElement().getElementId().getDomain(),
				action.getElement().getElementId().getId());
		System.out.println("testit");

		if (!element.getActive())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "element of action must be active");

		checkAction(action, element);

//		update location of car in db
		updateCarLocation(action, userBoundary, element);

		if (action.getType().toLowerCase().equals(ActionType.park.name())) {
			ElementBoundary parkingElement = parkOrDepart(element, userBoundary, false, action);
			saveAction(action);
			return parkingElement;
		}
		if (action.getType().toLowerCase().equals(ActionType.depart.name())) {
			ElementBoundary parkingElement = parkOrDepart(element, userBoundary, true, action);
			saveAction(action);
			return parkingElement;
		}

		if (action.getType().toLowerCase().equals(ActionType.search.name())) {
			ElementBoundary elementArr[] = search(element, userBoundary, 0.02, action);
			saveAction(action);
			return elementArr;
		}

		saveAction(action);
		return action;

	}

	public void updateCarLocation(ActionBoundary action, UserBoundary ue, ElementBoundary element) {
		HashMap<String, Double> location = action.getActionAttributes().containsKey("location")
				? (HashMap<String, Double>) action.getActionAttributes().get("location")
				: new HashMap<>();

		if (!location.isEmpty())
			element.setLocation(new Location(location.get("lat"), location.get("lng")));

		toManager(ue);
		elementService.update(ue.getUserId().getDomain(), ue.getUserId().getEmail(), element.getElementId().getDomain(),
				element.getElementId().getId(), element);
		toPlayer(ue);
	}

	public void saveAction(ActionBoundary action) {
		ActionIdBoundary aib = new ActionIdBoundary(projectName, UUID.randomUUID().toString());
		action.setCreatedTimestamp(new Date());
		action.setActionId(aib);
		ActionEntity entity = converter.toEntity(action);
		this.actionDao.save(entity);
	}

	public ElementBoundary[] search(ElementBoundary car, UserBoundary user, double distance, ActionBoundary action) {

		return Stream
				.concat(Arrays.stream(elementService.searchByLocationAndType(user.getUserId().getDomain(),
						user.getUserId().getEmail(), car.getLocation().getLat(), car.getLocation().getLng(), distance,
						ElementType.parking.name(), 36, 0).toArray(new ElementBoundary[0])),
						Arrays.stream(elementService.searchByLocationAndType(user.getUserId().getDomain(),
								user.getUserId().getEmail(), car.getLocation().getLat(), car.getLocation().getLng(),
								distance, ElementType.parking_lot.name(), 36, 0).toArray(new ElementBoundary[0])))
				.toArray(ElementBoundary[]::new);
	}

	public void checkAction(ActionBoundary action, ElementBoundary element) {

		if (!element.getType().equals(ElementType.car.name())
				&& (action.getType().equals(ActionType.search.name()) || action.getType().equals(ActionType.park.name())
						|| action.getType().equals(ActionType.depart.name())))
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE,
					"park /search/ deprat can only invoke on car element");

	}

	public ElementBoundary parkOrDepart(ElementBoundary car, UserBoundary user, boolean depart, ActionBoundary action) {

		ElementBoundary parkingBoundary = null;
		double distanceFromCar = 0.0002;

		UserBoundary userBoundary = toManager(user);

		ElementBoundary[] parking = elementService
				.getAnArrayWithElementParent(user.getUserId().getDomain(), user.getUserId().getEmail(),
						car.getElementId().getDomain(), car.getElementId().getId(), 1, 0)
				.toArray(new ElementBoundary[0]);

//		if parking exist and no need in creating new parking representation 
		parkingBoundary = parkOrDepartValidation(depart, parking, car, user);

		if (parkingBoundary != null)
			return parkingBoundary;

//Searching for nearby parking to occupy 
		ElementBoundary[] parkingNearby = this.elementService.searchByLocationAndType(user.getUserId().getDomain(),
				user.getUserId().getEmail(), car.getLocation().getLat(), car.getLocation().getLng(), distanceFromCar,
				ElementType.parking.name(), 20, 0).toArray(new ElementBoundary[0]);

		ElementBoundary[] parkingLotNearBy = this.elementService.searchByLocationAndType(user.getUserId().getDomain(),
				user.getUserId().getEmail(), car.getLocation().getLat(), car.getLocation().getLng(),
				distanceFromCar * 4, ElementType.parking_lot.name(), 20, 0).toArray(new ElementBoundary[0]);

		if (parkingLotNearBy.length > 0)
			parkingBoundary = updateParkingLot(depart, car, userBoundary, parkingLotNearBy);
		else if (parkingNearby.length > 0)
			parkingBoundary = updateParking(car, depart, userBoundary, parkingNearby);

//		if we didn't found parking nearby -> create new one
		if (parkingBoundary == null)
			parkingBoundary = createParking(car, depart, userBoundary);

//		Bind each car to parking or parking-lot
//TODO - bind only on park in case of parking lot
//
//		if (depart) {
//			ElementEntity carEntity = elementDao
//					.findById(new ElementIdEntity(car.getElementId().getDomain(), car.getElementId().getId()))
//					.orElseThrow(() -> new ObjectNotFoundException("could not find object by elementDomain: "
//							+ car.getElementId().getDomain() + "or elementId: " + car.getElementId().getId()));
//
//			HashSet<ElementEntity> allCars = (HashSet<ElementEntity>) parkingEntity.getResponses();
//			allCars.remove(carEntity);
//			parkingEntity.setResponses(allCars);
//			carEntity.setParent(null);
//			this.elementDao.save(carEntity);
//		}
//		ElementEntity carEntity = elementDao
//				.findById(new ElementIdEntity(car.getElementId().getDomain(), car.getElementId().getId()))
//				.orElseThrow(() -> new ObjectNotFoundException("could not find object by elementDomain: "
//						+ car.getElementId().getDomain() + "or elementId: " + car.getElementId().getId()));
//
//		ElementEntity parkingEntity = elementDao
//				.findById(new ElementIdEntity(parkingBoundary.getElementId().getDomain(),
//						parkingBoundary.getElementId().getId()))
//				.orElseThrow(() -> new ObjectNotFoundException("could not find object by elementDomain: "
//						+ car.getElementId().getDomain() + "or elementId: " + car.getElementId().getId()));
//
//		if (depart) {
//
//			HashSet<ElementEntity> allCars = (HashSet<ElementEntity>) parkingEntity.getResponses();
//			allCars.remove(carEntity);
//			parkingEntity.setResponses(allCars);
//			carEntity.setParent(null);
//			this.elementDao.save(carEntity);
//
//		} else {
//
//			this.elementService.bindExistingElementToAnExsitingChildElement(userBoundary.getUserId().getDomain(),
//					userBoundary.getUserId().getEmail(), parkingBoundary.getElementId(),
//					new ElementIdBoundary(car.getElementId().getDomain(), car.getElementId().getId()));
//		}

		unBindOrBindElements(parkingBoundary.getElementId(), car.getElementId(), depart, userBoundary);
		toPlayer(user);

		return parkingBoundary;
	}

	public void unBindOrBindElements(ElementIdBoundary parking, ElementIdBoundary car, boolean depart,
			UserBoundary userBoundary) {
//  	depart == true --> unbind
		if (depart) {
			ElementEntity carEntity = elementDao.findById(new ElementIdEntity(car.getDomain(), car.getId()))
					.orElseThrow(() -> new ObjectNotFoundException("could not find object by elementDomain: "
							+ car.getDomain() + "or elementId: " + car.getId()));

			ElementEntity parkingEntity = elementDao.findById(new ElementIdEntity(parking.getDomain(), parking.getId()))
					.orElseThrow(() -> new ObjectNotFoundException("could not find object by elementDomain: "
							+ parking.getDomain() + "or elementId: " + parking.getId()));

			Set<ElementEntity> allCars = (Set<ElementEntity>) parkingEntity.getResponses();
			allCars.remove(carEntity);
			parkingEntity.setResponses(allCars);
			carEntity.setParent(null);
			this.elementDao.save(carEntity);

//		depart == false --> bind
		} else
			this.elementService.bindExistingElementToAnExsitingChildElement(userBoundary.getUserId().getDomain(),
					userBoundary.getUserId().getEmail(), parking, car);

	}

	public ElementBoundary parkOrDepartValidation(boolean depart, ElementBoundary[] parking, ElementBoundary car,
			UserBoundary user) {
		ElementIdBoundary lastCar = null;
		ElementBoundary parkingBoundary = null;

		if (parking.length <= 0)
			return null;

//		TODO - check if the car bind to parking
		if (parking[0].getElementAttributes().containsKey("LastCarReport")) {
			HashMap<String, String> myMap;
			myMap = (HashMap<String, String>) parking[0].getElementAttributes().get("LastCarReport");

			lastCar = new ElementIdBoundary(myMap.get("domain"), myMap.get("id"));
		}
//		check if user already parking - not allowed 
		if (parking.length > 0 && !depart)
			if (!parking[0].getActive())
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
						"You cannot park when you are already parked ;<");

//		check if user need to depart specific parking
		if (parking.length > 0 && depart)
			if (!parking[0].getActive() && areEqual(car.getElementId(), lastCar)) {

				if (parking[0].getType().equals(ElementType.parking.name())) {
					parkingBoundary = updateParking(car, depart, user, parking[0]);
					unBindOrBindElements(parkingBoundary.getElementId(), car.getElementId(), depart, user);
				}
				toPlayer(user);
				return parkingBoundary;
			}
		return parkingBoundary;
	}

	public boolean areEqual(ElementIdBoundary elementId_1, ElementIdBoundary elementId_2) {

		if (elementId_1 == null || elementId_2 == null)
			return false;

		if (elementId_1.getDomain() != elementId_2.getDomain() || elementId_1.getId() != elementId_2.getId())
			return false;

		return true;

	}

	public UserBoundary toPlayer(UserBoundary user) {
		user.setRole(UserRole.PLAYER);
		return userService.updateUser(user.getUserId().getDomain(), user.getUserId().getEmail(), user);
//		return converter.fromEntity(this.userDao.save(user));

	}

	public UserBoundary toManager(UserBoundary user) {
		user.setRole(UserRole.MANAGER);

		return userService.updateUser(user.getUserId().getDomain(), user.getUserId().getEmail(), user);

	}

	public ElementBoundary updateParking(ElementBoundary car, boolean depart, UserBoundary userBoundary,
			ElementBoundary... parkingNearby) {

		ElementBoundary parkingBoundary = ServiceTools.getClosest(car, parkingNearby);

		ElementEntity parkingEntity = elementDao
				.findById(new ElementIdEntity(parkingBoundary.getElementId().getDomain(),
						parkingBoundary.getElementId().getId()))
				.orElseThrow(() -> new ObjectNotFoundException("could not find object by elementDomain: "
						+ car.getElementId().getDomain() + "or elementId: " + car.getElementId().getId()));

		parkingEntity.getElementAttributes().put("LastCarReport",
				new ElementIdBoundary(car.getElementId().getDomain(), car.getElementId().getId()));

		parkingEntity.getElementAttributes().put("lastReportTimestamp", new Date());

		parkingEntity.setActive(depart);

//		this.elementService.update(userBoundary.getUserId().getDomain(), userBoundary.getUserId().getEmail(),
//				parkingBoundary.getElementId().getDomain(), parkingBoundary.getElementId().getId(), parkingBoundary);
		return converter.fromEntity(this.elementDao.save(parkingEntity));
	}

	public ElementBoundary createParking(ElementBoundary car, boolean depart, UserBoundary userBoundary) {

		HashMap<String, Object> currentParkingAttributes = new HashMap<>();
		currentParkingAttributes.put("LastCarReport",
				new ElementIdBoundary(car.getElementId().getDomain(), car.getElementId().getId()));
		currentParkingAttributes.put("lastReportTimestamp", new Date());

		ElementBoundary parkingBoundary = new ElementBoundary(new ElementIdBoundary("", ""), ElementType.parking.name(),
				"parking_name", depart, new Date(), car.getLocation(), currentParkingAttributes, car.getCreatedBy());

		return this.elementService.create(userBoundary.getUserId().getDomain(), userBoundary.getUserId().getEmail(),
				parkingBoundary);
	}

	public ElementBoundary updateParkingLot(Boolean depart, ElementBoundary car, UserBoundary userBoundary,
			ElementBoundary... parkingLotNearBy) {

		ElementBoundary parkingBoundary = ServiceTools.getClosest(car, parkingLotNearBy);

		ElementIdBoundary[] carArray = new ElementIdBoundary[1];
		ArrayList<ElementIdBoundary> carList = new ArrayList<>();
		int counter = 0;

		if (parkingBoundary.getElementAttributes().isEmpty()) {
			HashMap<String, Object> myMap = new HashMap<>();
			myMap.put("carList", carArray);
			myMap.put("capacity", 80);
			myMap.put("carCounter", 0);
			carList.add(car.getElementId());
			myMap.put("carList", carList.toArray(new ElementIdBoundary[0]));
			parkingBoundary.setElementAttributes(myMap);

		}

		if (parkingBoundary.getElementAttributes().containsKey("carList")) {
			carArray = (ElementIdBoundary[]) parkingBoundary.getElementAttributes().get("carList");
			if (carArray.length > 0)
				if (!carList.contains(carArray[0]))
					carList.add(carArray[0]);
				else
					throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
							"You cannot park when you are already parked ;<");

		} else {
			carList.add(car.getElementId());
			parkingBoundary.getElementAttributes().put("carList", carList.toArray(new ElementIdBoundary[0]));
		}

		List<ElementIdBoundary> tempList = Arrays.asList(carArray);
		carList.addAll(tempList);

//		project require parking lots to have capacity
		if (!parkingBoundary.getElementAttributes().containsKey("capacity"))
			parkingBoundary.getElementAttributes().put("capacity", 80);

		if (parkingBoundary.getElementAttributes().containsKey("carCounter"))
			counter = (int) parkingBoundary.getElementAttributes().get("carCounter");

		else if (depart) {
			if (counter > 0 && carList.contains(car.getElementId())) {
				parkingBoundary.getElementAttributes().put("carCounter", counter - 1);

				carList.remove(car.getElementId());
			}

		} else {
			parkingBoundary.getElementAttributes().put("carCounter", 1);
			carList.add(new ElementIdBoundary(car.getElementId().getDomain(), car.getElementId().getId()));
		}
		if (!carList.isEmpty())
			parkingBoundary.getElementAttributes().put("carList", carList.toArray(carArray));

		if ((int) parkingBoundary.getElementAttributes().get("carCounter")
				+ 1 > (int) parkingBoundary.getElementAttributes().get("capacity"))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "this parking lot is full");

//		parking lot is full - not active
		if ((int) parkingBoundary.getElementAttributes().get("carCounter")
				+ 2 > (int) parkingBoundary.getElementAttributes().get("capacity"))
			parkingBoundary.setActive(false);

		parkingBoundary.getElementAttributes().put("carCounter", counter + 1);

		parkingBoundary.getElementAttributes().put("lastReportTimestamp", new Date());

		return this.elementService.update(userBoundary.getUserId().getDomain(), userBoundary.getUserId().getEmail(),
				parkingBoundary.getElementId().getDomain(), parkingBoundary.getElementId().getId(), parkingBoundary);

	}

	@Override
	@Transactional(readOnly = true)
	public List<ActionBoundary> getAllActions(String adminDomain, String adminEmail) {

		ServiceTools.stringValidation(adminDomain, adminEmail);

		Iterable<ActionEntity> allActions = this.actionDao.findAll();

		List<ActionBoundary> rv = new ArrayList<>();
		for (ActionEntity ent : allActions)
			rv.add(this.converter.fromEntity(ent));

		return rv;

	}

	@Override
	@Transactional // (readOnly = false)
	public void deleteAllActions(String adminDomain, String adminEmail) {

		ServiceTools.stringValidation(adminDomain, adminEmail);

		UserBoundary uE = this.userService.login(adminDomain, adminEmail);

		if (!uE.getRole().equals(UserRole.ADMIN))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "only admin can delete all actions");

		this.actionDao.deleteAll();

	}

	@Override
	public List<ActionBoundary> getAllActions(String adminDomain, String adminEmail, int size, int page) {

		ServiceTools.stringValidation(adminDomain, adminEmail);

		UserBoundary uE = this.userService.login(adminDomain, adminEmail);

		if (!uE.getRole().equals(UserRole.ADMIN))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "only admin can get all actions");

		ServiceTools.validatePaging(size, page);

		return this.actionDao.findAll(PageRequest.of(page, size, Direction.DESC, "actionId"))// Page<ActionEntity>
				.getContent()// List<ActionEntity>
				.stream()// Stream<ActionEntity>
				.map(this.converter::fromEntity)// Stream<ActionEntity>
				.collect(Collectors.toList()); // List<ActionEntity>

	}
}
