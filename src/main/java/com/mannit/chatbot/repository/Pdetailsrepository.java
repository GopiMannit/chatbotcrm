/*
 * package com.mannit.chatbot.repository;
 * 
 * import java.util.List;
 * 
 * import org.springframework.data.mongodb.repository.MongoRepository; import
 * org.springframework.data.mongodb.repository.Query; import
 * org.springframework.stereotype.Repository;
 * 
 * import com.mannit.chatbot.model.PatientDetails;
 * 
 * 
 * @Repository public interface Pdetailsrepository extends
 * MongoRepository<PatientDetails, String> {
 * 
 * @Query("{Date : {$gte : ?0, $lte : ?1}}}") List<PatientDetails>
 * findBydate(String endDate,String startDate);
 * 
 * @Query("{Date:?0}") List<PatientDetails> findAllBydate(String date); }
 */