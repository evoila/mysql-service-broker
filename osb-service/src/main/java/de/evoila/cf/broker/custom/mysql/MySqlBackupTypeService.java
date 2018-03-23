package de.evoila.cf.broker.custom.mysql;

import de.evoila.cf.broker.service.BackupTypeService;
import de.evoila.cf.model.enums.DatabaseType;
import org.springframework.stereotype.Service;

@Service
public class MySqlBackupTypeService implements BackupTypeService{
    @Override
    public DatabaseType getType () {
        return DatabaseType.MySQL;
    }
}
