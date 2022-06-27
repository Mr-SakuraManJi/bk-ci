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

package com.tencent.devops.stream.trigger.git.pojo.github

import com.tencent.devops.common.sdk.github.response.PullRequestFileResponse
import com.tencent.devops.repository.pojo.git.GitMrChangeInfo
import com.tencent.devops.scm.pojo.ChangeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitChangeFileInfo

data class GithubChangeFileInfo(
    override val oldPath: String,
    override val newPath: String,
    override val renameFile: Boolean,
    override val deletedFile: Boolean
) : StreamGitChangeFileInfo {

    constructor(c: ChangeFileInfo) : this(
        oldPath = c.oldPath,
        newPath = c.newPath,
        renameFile = c.renameFile,
        deletedFile = c.deletedFile
    )

    constructor(c: GitMrChangeInfo.GitMrFile) : this(
        oldPath = c.oldPath,
        newPath = c.newPath,
        renameFile = c.renameFile,
        deletedFile = c.deletedFile
    )

    constructor(c: PullRequestFileResponse) : this(
        // todo 注意属性是否正确
        oldPath = c.previousFilename ?: "",
        newPath = c.filename,
        renameFile = c.status == "renamed",
        deletedFile = c.status == "removed"
    )
}
