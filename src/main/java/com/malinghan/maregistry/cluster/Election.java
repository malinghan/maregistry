//
// Election.java
// 主从选举算法类
//
// 该类实现基于hashCode的确定性选举算法，确保所有节点选出相同的Leader
//

package com.malinghan.maregistry.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主从选举算法类
 * 
 * 实现简单但确定性的Leader选举算法：
 * - 基于节点URL的hashCode进行选举
 * - 选择hashCode最小的在线节点作为Leader
 * - 保证所有节点得出相同的选举结果
 * 
 * 算法特点：
 * - 确定性：相同输入总是产生相同输出
 * - 简单高效：时间复杂度O(n)
 * - 无需额外协调：各节点独立计算得出相同结果
 * 
 * 适用场景：
 * - 小规模集群（3-5个节点）
 * - 网络相对稳定的环境
 * - 对选举速度要求较高的场景
 */
@Component
public class Election {
    
    private static final Logger log = LoggerFactory.getLogger(Election.class);
    
    @Autowired
    private Cluster cluster;
    
    /**
     * 执行Leader选举
     * 
     * 算法流程：
     * 1. 获取所有在线节点
     * 2. 计算每个节点URL的hashCode
     * 3. 选择hashCode最小的节点作为Leader
     * 4. 更新集群的Leader信息
     * 
     * @return 选举产生的Leader节点，如果没有合适节点则返回null
     */
    public Server electLeader() {
        List<Server> onlineServers = cluster.getOnlineServers();
        
        if (onlineServers.isEmpty()) {
            log.warn("没有在线节点，无法进行选举");
            cluster.setLeader(null);
            return null;
        }
        
        if (onlineServers.size() == 1) {
            Server soleServer = onlineServers.get(0);
            log.info("只有一个在线节点，自动成为Leader: {}", soleServer.getUrl());
            cluster.setLeader(soleServer);
            return soleServer;
        }
        
        // 执行选举算法
        Server electedLeader = performElection(onlineServers);
        
        if (electedLeader != null) {
            cluster.setLeader(electedLeader);
            log.info("选举完成，新的Leader: {}", electedLeader.getUrl());
        } else {
            log.error("选举失败，未选出合适的Leader");
        }
        
        return electedLeader;
    }
    
    /**
     * 执行具体的选举算法
     * 
     * @param candidates 候选节点列表
     * @return 选举出的Leader节点
     */
    private Server performElection(List<Server> candidates) {
        Server leaderCandidate = null;
        int minHashCode = Integer.MAX_VALUE;
        
        log.debug("开始选举，候选节点数: {}", candidates.size());
        
        for (Server candidate : candidates) {
            // 计算节点URL的hashCode
            int hashCode = candidate.getUrl().hashCode();
            
            log.debug("节点 {} 的hashCode: {}", candidate.getUrl(), hashCode);
            
            // 选择hashCode最小的节点
            if (hashCode < minHashCode) {
                minHashCode = hashCode;
                leaderCandidate = candidate;
            }
        }
        
        return leaderCandidate;
    }
    
    /**
     * 验证选举结果的一致性
     * 
     * 在分布式环境中验证所有节点是否选择了相同的Leader
     * 
     * @param expectedLeader 期望的Leader节点
     * @return 选举结果是否一致
     */
    public boolean validateElection(Server expectedLeader) {
        if (expectedLeader == null) {
            return cluster.getLeader() == null;
        }
        
        Server actualLeader = cluster.getLeader();
        if (actualLeader == null) {
            return false;
        }
        
        boolean consistent = expectedLeader.getUrl().equals(actualLeader.getUrl());
        if (!consistent) {
            log.warn("选举结果不一致 - 期望: {}, 实际: {}", 
                    expectedLeader.getUrl(), actualLeader.getUrl());
        }
        
        return consistent;
    }
    
    /**
     * 检查是否需要重新选举
     * 
     * 当前Leader离线或集群状态发生变化时需要重新选举
     * 
     * @return 是否需要重新选举
     */
    public boolean shouldReelect() {
        Server currentLeader = cluster.getLeader();
        
        // 没有Leader需要选举
        if (currentLeader == null) {
            log.debug("当前无Leader，需要选举");
            return true;
        }
        
        // 当前Leader离线需要重新选举
        if (!currentLeader.isStatus()) {
            log.info("当前Leader {} 已离线，需要重新选举", currentLeader.getUrl());
            return true;
        }
        
        // 检查是否存在多个声称是Leader的节点（脑裂情况）
        long leaderCount = cluster.getServers().stream()
                .filter(Server::isStatus)  // 只统计在线节点
                .filter(Server::isLeader)
                .count();
        
        if (leaderCount > 1) {
            log.warn("检测到{}个多Leader情况，需要重新选举", leaderCount);
            return true;
        }
        
        log.debug("当前Leader状态正常，无需重新选举");
        return false;
    }
    
    /**
     * 强制重新选举
     * 
     * 清除当前Leader状态并执行新的选举
     * 
     * @return 新选举的Leader节点
     */
    public Server forceReelection() {
        log.info("执行强制重新选举");
        cluster.setLeader(null);
        return electLeader();
    }
    
    /**
     * 获取选举统计信息
     * 
     * @return 选举相关信息的字符串描述
     */
    public String getElectionInfo() {
        Server leader = cluster.getLeader();
        List<Server> onlineServers = cluster.getOnlineServers();
        
        return String.format("选举信息 - 在线节点: %d, 当前Leader: %s", 
                onlineServers.size(), 
                leader != null ? leader.getUrl() : "无");
    }
}