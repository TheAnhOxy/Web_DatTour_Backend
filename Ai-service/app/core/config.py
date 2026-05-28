from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    GEMINI_API_KEY: str = ""
    AI_MODE: str = "auto"
    SEARCH_SERVICE_URL: str = "http://localhost:8083/search/tours"
    MONGODB_URL: str = "mongodb://localhost:27017"
    DATABASE_NAME: str = "webtour_db"
    REDIS_URL: str = "redis://localhost:6379/0"
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:29092,kafka:9092"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

settings = Settings()
