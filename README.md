# File storage container

## Configuration

#### `admin_token` - Bearer Authorization of the media to be able to save and delete files by the main server
#### `storage_folder` - The path where the files are stored
#### `file_size_limit` - Maximum file size

#### `client_token` - Bearer Authorization of the carrier for requests to the main server, for the request it is necessary to save files
#### `exists_request` - Request with a question about storing a file to the main server. If the file is needed, then return status 200
#### `repeat_time` - How often to ask
#### `old_mils` - Ask about files older than. If the server returns 200, the date is reset, if 404, it is deleted.

```
ktor {
    deployment {
        port  = 8083
    }
    application {
        modules = [ com.uogames.file.storage.ApplicationKt.module ]
    }
    admin_token = "..."
    storage_folder = "..."
    file_size_limit = 100_000_000

    clean_up {
        client_token = "..."
        exists_request = "http://localhost:8080/exists"
        repeat_time = 60_000
        old_mils = 600_000
    }

}
```
