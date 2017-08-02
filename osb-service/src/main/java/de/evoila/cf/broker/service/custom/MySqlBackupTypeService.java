package de.evoila.cf.broker.service.custom;

import de.evoila.cf.broker.service.BackupTypeService;
import org.springframework.stereotype.Service;

@Service
public class MySqlBackupTypeService implements BackupTypeService{
    @Override
    public String getType () {
        return "MySQL";
    }
}
