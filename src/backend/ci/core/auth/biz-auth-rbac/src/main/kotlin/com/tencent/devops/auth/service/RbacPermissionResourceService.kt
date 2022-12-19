/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateSubsetManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.entity.AuthResourceInfo
import com.tencent.devops.auth.enums.ResourceGroupType
import com.tencent.devops.auth.pojo.enum.GroupMemberStatus
import com.tencent.devops.auth.pojo.vo.GroupInfoVo
import com.tencent.devops.auth.pojo.vo.GroupMemberInfoVo
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionScopesService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory

class RbacPermissionResourceService(
    private val client: Client,
    private val permissionScopesService: PermissionScopesService,
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceService: AuthResourceService,
    private val groupService: AuthGroupService,
    private val strategyService: StrategyService
) : PermissionResourceService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceService::class.java)
    }

    override fun resourceCreateRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        val name = IamGroupUtils.buildSubsetManagerGroupName(
            resourceName = resourceName,
            groupDisplayName = ResourceGroupType.OWNER.displayName
        )
        val description = IamGroupUtils.buildSubsetManagerDescription(
            resourceName = resourceName,
            userId = userId
        )
        val projectInfo = getProjectInfo(projectCode)
        val authorizationScopes = permissionScopesService.buildSubsetManagerAuthorizationScopes(
            strategyName = ResourceGroupType.OWNER.getStrategyName(resourceType = resourceType),
            projectCode = projectCode,
            projectName = projectInfo.projectName,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
        val createSubsetManagerDTO = CreateSubsetManagerDTO.builder()
            .name(name)
            .description(description)
            .members(listOf(userId))
            .authorizationScopes(authorizationScopes)
            .inheritSubjectScope(false)
            .subjectScopes(listOf())
            .build()
        val subsetManagerId = iamV2ManagerService.createSubsetManager(
            projectInfo.relationId!!,
            createSubsetManagerDTO
        )
        authResourceService.create(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            relationId = subsetManagerId.toString()
        )
        return true
    }

    override fun hasManagerPermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        // 1. 先判断是否是项目管理员

        // 2. 判断是否是资源管理员
        return true
    }

    override fun isEnablePermission(
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        return getResourceInfo(
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).enable
    }

    override fun listGroup(
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): List<GroupInfoVo> {
        val resourceInfo = getResourceInfo(
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 10
        val iamGroupInfos =
            iamV2ManagerService.getSubsetManagerRoleGroup(resourceInfo.relationId.toInt(), pageInfoDTO)
        val iamGroupInfoMap = iamGroupInfos.results.associateBy { it.id }
        val localGroupInfos = groupService.getGroupByRelationIds(iamGroupInfoMap.keys.toList())
        return localGroupInfos.map {
            val userCount = iamGroupInfoMap[it.relationId.toInt()]?.userCount ?: 0
            val departmentCount = iamGroupInfoMap[it.relationId.toInt()]?.departmentCount ?: 0
            GroupInfoVo(
                id = it.id,
                name = it.groupName,
                displayName = it.displayName,
                code = it.groupCode,
                defaultRole = it.groupType,
                userCount = userCount,
                departmentCount = departmentCount
            )
        }.toList()
    }

    override fun listUserBelongGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): List<GroupMemberInfoVo> {
        val resourceInfo = getResourceInfo(
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 10
        val iamGroupInfos =
            iamV2ManagerService.getSubsetManagerRoleGroup(resourceInfo.relationId.toInt(), pageInfoDTO)
        val iamIds = iamGroupInfos.results.map { it.id }
        val verifyResult =
            iamV2ManagerService.verifyGroupValidMember(userId, iamIds.joinToString(",")).verifyResult
        val localGroupInfos = groupService.getGroupByRelationIds(iamIds).associateBy { it.relationId }
        val groupMemberInfoList = mutableListOf<GroupMemberInfoVo>()
        verifyResult.forEach { (iamGroupId, result) ->
            val localGroupInfo = localGroupInfos[iamGroupId.toString()] ?: return@forEach
            // TODO 需验证下,如果没有加入,返回值为啥
            val (status, createTime, expiredTime) = if (result.belong) {
                val status = GroupMemberStatus.NORMAL.name
                val createTime = ""
                val expiredTime = ""
                Triple(status, createTime, expiredTime)
            } else {
                val status = GroupMemberStatus.NOT_JOINED.name
                val createTime = ""
                val expiredTime = ""
                Triple(status, createTime, expiredTime)
            }
            groupMemberInfoList.add(
                GroupMemberInfoVo(
                    userId = userId,
                    groupId = localGroupInfo.id,
                    groupName = localGroupInfo.displayName,
                    createdTime = createTime,
                    status = status,
                    expiredTime = expiredTime
                )
            )
        }
        return groupMemberInfoList
    }

    override fun getGroupPolicies(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): List<String> {
        val groupInfo = groupService.getGroupCode(groupId = groupId) ?: return emptyList()
        val policies = mutableListOf<String>()
        strategyService.getStrategyByName(
            ResourceGroupType.getStrategyName(
                resourceType = resourceType,
                groupCode = groupInfo.groupCode
            )
        )?.strategy?.forEach { policies.addAll(it.value) }
        return policies
    }

    override fun enableResourcePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("enable resource permission|$userId|$projectId|$resourceType|$resourceCode")
        return authResourceService.enable(
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    override fun disableResourcePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("disable resource permission|$userId|$projectId|$resourceType|$resourceCode")
        return authResourceService.disable(
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    override fun renewal(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("renewal group member|$userId|$projectId|$resourceType|$groupId")
        val groupInfo = groupService.getGroupCode(groupId) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.GROUP_NOT_EXIST,
            params = arrayOf(groupId.toString()),
            defaultMessage = "权限系统： 用户组[$groupId]不存在"
        )
        val managerMember = ManagerMember(ManagerScopesEnum.USER.name, userId)
        val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
            .members(listOf(managerMember))
            .expiredAt(365)
            .build()
        iamV2ManagerService.renewalRoleGroupMemberV2(
            groupInfo.relationId.toInt(),
            managerMemberGroupDTO
        )
        return true
    }

    override fun delete(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Boolean {
        logger.info("delete group member|$userId|$projectId|$resourceType|$groupId")
        val groupInfo = groupService.getGroupCode(groupId) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.GROUP_NOT_EXIST,
            params = arrayOf(groupId.toString()),
            defaultMessage = "权限系统： 用户组[$groupId]不存在"
        )
        iamV2ManagerService.deleteRoleGroupMemberV2(
            groupInfo.relationId.toInt(),
            ManagerScopesEnum.USER.name,
            userId
        )
        return true
    }

    private fun getProjectInfo(projectCode: String): ProjectVO {
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = projectCode).data ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectCode),
                defaultMessage = "项目[$projectCode]不存在"
            )
        projectInfo.relationId ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
            params = arrayOf(projectCode),
            defaultMessage = "权限系统：[$projectCode]绑定系统资源为空"
        )
        return projectInfo
    }

    private fun getResourceInfo(
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): AuthResourceInfo {
        return authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RESOURCE_NOT_FOUND
        )
    }
}
