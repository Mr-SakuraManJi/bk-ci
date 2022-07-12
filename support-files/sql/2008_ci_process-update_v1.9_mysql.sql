USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'PLATFORM_CODE') THEN
        alter table T_PIPELINE_BUILD_TASK add column `PLATFORM_CODE` varchar(64) DEFAULT NULL COMMENT '对接平台代码';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_BUILD_TASK'
                        AND COLUMN_NAME = 'PLATFORM_ERROR_CODE') THEN
        alter table T_PIPELINE_BUILD_TASK add column `PLATFORM_ERROR_CODE` int(11) DEFAULT NULL COMMENT '对接平台错误码';
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_SETTING'
                        AND COLUMN_NAME = 'CLEAN_VARIABLES_WHEN_RETRY') THEN
        ALTER TABLE T_PIPELINE_SETTING ADD COLUMN `CLEAN_VARIABLES_WHEN_RETRY` BIT(1) DEFAULT b'0' COMMENT '重试时清理变量表';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW'
                    AND INDEX_NAME = 'IDX_PROJECT_ID') THEN
        ALTER TABLE T_PIPELINE_VIEW ADD INDEX `IDX_PROJECT_ID` (`PROJECT_ID`);
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_VIEW'
                    AND INDEX_NAME = 'PROJECT_NAME') THEN
        ALTER TABLE T_PIPELINE_VIEW DROP INDEX `PROJECT_NAME`;
    END IF;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_PIPELINE_VIEW'
                        AND COLUMN_NAME = 'VIEW_TYPE') THEN
        ALTER TABLE T_PIPELINE_VIEW ADD COLUMN `VIEW_TYPE` int NOT NULL DEFAULT '1' COMMENT '1:动态流水线组 , 2:静态流水线组';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
