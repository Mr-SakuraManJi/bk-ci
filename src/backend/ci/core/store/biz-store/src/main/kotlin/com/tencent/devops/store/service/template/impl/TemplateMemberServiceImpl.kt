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
 */

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.pojo.common.STORE_MEMBER_ADD_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.impl.StoreMemberServiceImpl
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service("templateMemberService")
class TemplateMemberServiceImpl : StoreMemberServiceImpl() {

    @Autowired
    private lateinit var marketTemplateDao: MarketTemplateDao

    private val executorService = Executors.newFixedThreadPool(5)

    private val logger = LoggerFactory.getLogger(TemplateMemberServiceImpl::class.java)

    /**
     * 添加模板成员
     */
    override fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean?,
        sendNotify: Boolean,
        checkPermissionFlag: Boolean,
        testProjectCode: String?
    ): Result<Boolean> {
        logger.info("addTemplateMember params:$userId|$storeMemberReq|$storeType|$collaborationFlag|$sendNotify")
        val templateCode = storeMemberReq.storeCode
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = templateCode,
                storeType = storeType.type.toByte()
            )
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val type = storeMemberReq.type.type.toByte()
        val receivers = mutableSetOf<String>()
        for (item in storeMemberReq.member) {
            if (storeMemberDao.isStoreMember(dslContext, item, templateCode, storeType.type.toByte())) {
                continue
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeMemberDao.addStoreMember(context, userId, templateCode, item, type, storeType.type.toByte())
            }
            receivers.add(item)
        }
        if (sendNotify) {
            executorService.submit<Result<Boolean>> {
                val bodyParams = mapOf("storeAdmin" to userId, "storeName" to getStoreName(templateCode))
                storeNotifyService.sendNotifyMessage(
                    templateCode = STORE_MEMBER_ADD_NOTIFY_TEMPLATE + "_$storeType",
                    sender = DEVOPS,
                    receivers = receivers,
                    bodyParams = bodyParams
                )
            }
        }
        return Result(true)
    }

    /**
     * 删除模板成员
     */
    override fun delete(
        userId: String,
        id: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean
    ): Result<Boolean> {
        logger.info("deleteTemplateMember params:[$userId|$id|$storeCode|$storeType|$checkPermissionFlag]")
        val memberRecord = storeMemberDao.getById(dslContext, id)
            ?: return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(id))
        // 如果删除的是管理员，只剩一个管理员则不允许删除
        if ((memberRecord.type).toInt() == 0) {
            val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
            if (validateAdminResult.isNotOk()) {
                return Result(status = validateAdminResult.status, message = validateAdminResult.message, data = false)
            }
        }
        return super.delete(
            userId = userId,
            id = id,
            storeCode = storeCode,
            storeType = storeType,
            checkPermissionFlag = checkPermissionFlag
        )
    }

    override fun getStoreName(storeCode: String): String {
        return marketTemplateDao.getLatestTemplateByCode(dslContext, storeCode)?.templateName ?: ""
    }
}
