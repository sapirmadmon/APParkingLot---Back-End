package acs.logic.implementation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
import acs.data.UserRoleEntityEnum;
import acs.logic.ActionService;
import acs.logic.EnhancedActionService;
import acs.logic.ObjectNotFoundException;
import acs.logic.ServiceTools;
import acs.rest.boundaries.action.ActionBoundary;
import acs.rest.boundaries.action.ActionIdBoundary;

@Service
public class DbActionServiceImplementation implements EnhancedActionService {
	private String projectName;
	private ActionDao actionDao;
	private Converter converter;
	private ElementDao elementDao;
	private UserDao userDao;

	@Autowired
	public DbActionServiceImplementation(ActionDao actionDao, ElementDao elementDao,UserDao userDao, Converter converter) {
		this.converter = converter;
		this.actionDao = actionDao;
		this.elementDao = elementDao;
		this.userDao = userDao;

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

		/*
		 * for (Object user : action.getInvokedBy().values()) { UserBoundary userB =
		 * (UserBoundary) user; if (!userB.getRole().equals(UserRole.PLAYER)) throw new
		 * ResponseStatusException(HttpStatus.UNAUTHORIZED,
		 * "Admin User Can't Search Elements By Location"); }
		 */

			/*for (Object user : action.getInvokedBy().values()) {
				UserBoundary userB = (UserBoundary) user;
				if (!userB.getRole().equals(UserRole.PLAYER))
					throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
							"Admin User Can't Search Elements By Location");
			}*/
//			String userDomain = ((Map<String, Object>)(((Map<String, Object>)action.getInvokedBy().get("invokedBy")).get("userId"))).get("domain").toString();
//			String userEmail = ((Map<String, Object>)(((Map<String, Object>)action.getInvokedBy().get("invokedBy")).get("userId"))).get("email").toString();
			
			UserEntity ue = this.userDao.findById(this.converter.toEntity(action.getInvokedBy().getUserId()))
					.orElseThrow(() -> new ObjectNotFoundException("could not find object by ElementDomain:"
							+ action.getInvokedBy().getUserId().getDomain() + " or ElementId:" +
							action.getInvokedBy().getUserId().getEmail()));
			
			ElementIdEntity elementIdOfAction = this.converter.fromElementIdBoundary(action.getElement().getElement());
			ElementEntity element = this.elementDao.findById(elementIdOfAction)
					.orElseThrow(() -> new ObjectNotFoundException("could not find object by ElementDomain:"
							+ elementIdOfAction.getElementDomain() + " or ElementId:" + elementIdOfAction.getId()));
			
		/*ElementIdEntity elementIdOfAction = this.converter.fromElementIdBoundary(action.getElement().getElement());
		ElementEntity element = this.elementDao.findById(elementIdOfAction)
				.orElseThrow(() -> new ObjectNotFoundException("could not find object by ElementDomain:"
						+ elementIdOfAction.getDomain() + " or ElementId:" + elementIdOfAction.getId()));*/

			if (element.getActive()&&ue.getRole().equals(UserRoleEntityEnum.player)) {
				ActionIdBoundary aib = new ActionIdBoundary(projectName, UUID.randomUUID().toString());
				action.setCreatedTimestamp(new Date());
				action.setActionId(aib);
				ActionEntity entity = converter.toEntity(action);
				// actionDao.put(action.getActionId().toString(), entity);
				this.actionDao.save(entity);
				return action;
			}
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "can't invoke action");

		/*if (element.getActive()) {
			ActionIdBoundary aib = new ActionIdBoundary(projectName, UUID.randomUUID().toString());
			action.setCreatedTimestamp(new Date());
			action.setActionId(aib);
			ActionEntity entity = converter.toEntity(action);
			// actionDao.put(action.getActionId().toString(), entity);
			this.actionDao.save(entity);
			return action;
		}
		throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "invoke action only on active element!");
*/
		/*
		 * ActionIdBoundary aib = new ActionIdBoundary(projectName,
		 * UUID.randomUUID().toString()); action.setCreatedTimestamp(new Date());
		 * action.setActionId(aib); ActionEntity entity = converter.toEntity(action); //
		 * actionDao.put(action.getActionId().toString(), entity);
		 * this.actionDao.save(entity); return action;
		 */
	}

	@Override
	@Transactional(readOnly = true)
	public List<ActionBoundary> getAllActions(String adminDomain, String adminEmail) {

		ServiceTools.stringValidation(adminDomain, adminEmail);
		Iterable<ActionEntity> allActions = this.actionDao.findAll();
		List<ActionBoundary> rv = new ArrayList<>();
		for (ActionEntity ent : allActions)
			rv.add(this.converter.fromEntity(ent));
//			return this.actionDao.values().stream().map(this.converter::fromEntity).collect(Collectors.toList());
		return rv;

	}

	@Override
	@Transactional // (readOnly = false)
	public void deleteAllActions(String adminDomain, String adminEmail) {

		ServiceTools.stringValidation(adminDomain, adminEmail);
		this.actionDao.deleteAll();

	}

	@Override
	public List<ActionBoundary> getAllActions(String adminDomain, String adminEmail, int size, int page) {

		ServiceTools.stringValidation(adminDomain, adminEmail);

		ServiceTools.validatePaging(size, page);

		return this.actionDao.findAll(PageRequest.of(page, size, Direction.DESC, "actionId"))// Page<ActionEntity>
				.getContent()// List<ActionEntity>
				.stream()// Stream<ActionEntity>
				.map(this.converter::fromEntity)// Stream<ActionEntity>
				.collect(Collectors.toList()); // List<ActionEntity>

	}
}
