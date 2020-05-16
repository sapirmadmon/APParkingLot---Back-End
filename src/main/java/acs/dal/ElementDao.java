package acs.dal;


import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import acs.data.ElementEntity;
import acs.data.ElementIdEntity;

//Create Read Update Delete - CRUD
public interface ElementDao extends PagingAndSortingRepository<ElementEntity, ElementIdEntity> {


 		//CrudRepository<ElementEntity, ElementIdEntity> 
	
	 

	// CrudRepository<ElementEntity, ElementIdEntity>
	// SELECT ... FROM ELEMENT WHERE ORIGIN_ID=?
	//public List<ElementEntity> findAllByParent_id(@Param("elementId") ElementIdEntity elementId, Pageable pageable);
////
//	// SELECT ... FROM DUMMIES WHERE TYPE=?
//	public List<DummyEntity> findAllByType(
//			@Param("type") TypeEntityEnum type, 
//			Pageable pageable);	

	public List<ElementEntity> findAllByLocation_LatBetweenAndLocation_LngBetweenAndActive(
			@Param("minLat") Double minLat, @Param("maxLat") Double maxLat, @Param("minLng") Double minLng,
			@Param("maxLng") Double maxLng, @Param("active") Boolean active, Pageable pageable);

	public List<ElementEntity> findAllByLocation_LatBetweenAndLocation_LngBetween(@Param("minLat") double minLat,
			@Param("maxLat") double maxLat, @Param("minLng") double minLng, @Param("maxLng") double maxLng,
			Pageable pageable);

	// CrudRepository<ElementEntity, ElementIdEntity>


	public List<ElementEntity> findAllByName(@Param("name") String name,Pageable pageable);	

	// SELECT ... FROM ELEMENTS WHERE ACTIVE=?
	public List<ElementEntity> findAllByActive(@Param("active") boolean active, Pageable pageable);

}
