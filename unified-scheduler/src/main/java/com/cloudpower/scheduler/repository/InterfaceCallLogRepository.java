package com.cloudpower.scheduler.repository;

import com.cloudpower.scheduler.entity.InterfaceCallLog;
import com.cloudpower.scheduler.enums.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口调用日志数据访问层
 * Interface Call Log Repository
 */
@Repository
public interface InterfaceCallLogRepository extends JpaRepository<InterfaceCallLog, Long> {

    /**
     * 根据接口配置ID分页查询调用日志
     * Find call logs by interface config ID with pagination
     */
    Page<InterfaceCallLog> findByInterfaceConfigIdOrderByCreatedTimeDesc(Long interfaceConfigId, Pageable pageable);

    /**
     * 根据系统名称分页查询调用日志
     * Find call logs by system name with pagination
     */
    Page<InterfaceCallLog> findBySystemNameOrderByCreatedTimeDesc(String systemName, Pageable pageable);

    /**
     * 根据调用状态查询调用日志
     * Find call logs by call status
     */
    List<InterfaceCallLog> findByCallStatusAndCreatedTimeAfter(CallStatus callStatus, LocalDateTime startTime);

    /**
     * 根据追踪ID查询调用日志
     * Find call logs by trace ID
     */
    List<InterfaceCallLog> findByTraceIdOrderByCreatedTimeAsc(String traceId);

    /**
     * 查询指定时间范围内的失败调用统计
     * Count failed calls within time range
     */
    @Query("SELECT COUNT(l) FROM InterfaceCallLog l WHERE l.callStatus = 'FAILED' " +
           "AND l.createdTime BETWEEN :startTime AND :endTime")
    long countFailedCallsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内各系统的调用统计
     * Get call statistics by system within time range
     */
    @Query("SELECT l.systemName, l.callStatus, COUNT(l) FROM InterfaceCallLog l " +
           "WHERE l.createdTime BETWEEN :startTime AND :endTime " +
           "GROUP BY l.systemName, l.callStatus")
    List<Object[]> getCallStatisticsBySystem(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的日志记录
     * Delete logs before specified time
     */
    void deleteByCreatedTimeBefore(LocalDateTime time);
}