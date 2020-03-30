package demo.user;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.RestController;

import demo.MessageBoundary;
import demo.NameNotFoundException;
import demo.TypeEnumRole;

@RestController
public class UserController {

	// Sapir - User related API - Login valid user
	@RequestMapping(path = "/acs/users/login/{userDomain}/{userEmail}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)

	public UserBoundary loginValidUser(@PathVariable("userDomain") String userDomain,
			@PathVariable("userEmail") String userEmail) {
		if (userDomain != null && !userDomain.trim().isEmpty() && userEmail != null && !userEmail.trim().isEmpty()) {
			// return new UserBoundary();
			UserBoundary ub = new UserBoundary();
			ub.setUserId(new UserIdBoundary(userDomain, userEmail));
			ub.setTypeRole(TypeEnumRole.PLAYER);
			ub.setUsername("Demo User");
			ub.setAvater(";-)");
			return ub;
		} else {
			throw new NameNotFoundException("Invalid user name/email");
		}
	}

	// Sapir - User related API - Create a new user
	@RequestMapping(path = "/acs/users", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public UserBoundary CreateNewUser(@RequestBody NewUserDetailsBoundary userDetails) {
		UserBoundary ub = new UserBoundary();
		ub.setUserId(new UserIdBoundary("2020b.demo", userDetails.getEmail()));
		ub.setTypeRole(userDetails.getTypeRole());
		ub.setUsername(userDetails.getUsername());
		ub.setAvater(userDetails.getAvater());
		return ub;
	}

	// Sapir - User related API - Update user details
	@RequestMapping(path = "/acs/users/{userDomain}/{userEmail}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public MessageBoundary updateUserDetails(@PathVariable("userDomain") String userDomain,
			@PathVariable("userEmail") String userEmail, @RequestBody UserBoundary update) {
		return new MessageBoundary("Update");
	}

}
