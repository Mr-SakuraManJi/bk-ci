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

package com.tencent.devops.process.yaml.v2.models

import com.devops.process.yaml.v2.models.Concurrency
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v2.models.step.PreStep

/**
 * model
 *
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(stream,prebuild等)异常
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreScriptBuildYaml(
    var version: String?,
    var name: String?,
    var label: List<String>? = null,
    var triggerOn: PreTriggerOn?,
    var variables: Map<String, Variable>? = null,
    var stages: List<PreStage>? = null,
    var jobs: Map<String, PreJob>? = null,
    var steps: List<PreStep>? = null,
    var extends: Extends? = null,
    var resources: Resources?,
    var notices: List<GitNotices>?,
    var finally: Map<String, PreJob>? = null,
    val concurrency: Concurrency? = null
)