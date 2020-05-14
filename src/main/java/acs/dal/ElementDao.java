package acs.dal;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import acs.data.ElementEntity;
import acs.data.ElementIdEntity;

//Create Read Update Delete - CRUD
public interface ElementDao extends PagingAndSortingRepository<ElementEntity, ElementIdEntity> {
	public List<ElementEntity> findAllByLocation_LatBetweenAndLocation_LngBetweenAndActive(
			@Param("minLat") Double minLat, @Param("maxLat") Double maxLat, @Param("minLng") Double minLng,
			@Param("maxLng") Double maxLng, @Param("active") Boolean active, Pageable pageable);

	public List<ElementEntity> findAllByLocation_LatBetweenAndLocation_LngBetween(@Param("minLat") double minLat,
			@Param("maxLat") double maxLat, @Param("minLng") double minLng, @Param("maxLng") double maxLng,
			Pageable pageable);

}
