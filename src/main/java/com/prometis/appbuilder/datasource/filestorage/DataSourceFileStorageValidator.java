package com.prometis.appbuilder.datasource.filestorage;

import com.prometis.appbuilder.datasource.ConnectValidation;
import com.prometis.appbuilder.datasource.LookupKey;
import com.prometis.appbuilder.executor.file.FileStorage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataSourceFileStorageValidator {
    public ConnectValidation validateConnect(DataSourceFileStorage metadata, Map<LookupKey, FileStorage> dataSourceMap) {
        ConnectValidation validate = new ConnectValidation();

        LookupKey lookupKey = LookupKey.generateKey(metadata.getDataSourceName());

        FileStorage fileStorage = dataSourceMap.get(lookupKey);

        try {
            if(fileStorage == null) {
                throw new IllegalStateException("등록(갱신)되지않은 데이터소스입니다. [" + metadata.getDataSourceName() + "]");
            }

            fileStorage.validate();
            validate.setResult(true);
        }
        catch (Exception e) {
            validate.setResult(false);
            validate.setErrorStack(e);
        }

        return validate;
    }
}
