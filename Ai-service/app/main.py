from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.database import connect_to_mongo, close_mongo_connection
from app.services.memory_service import memory_service
from app.services.kafka_service import kafka_service
from app.api import chat

app = FastAPI(title="GoTourNow AI Service", version="1.0.0")

# CORS middleware is handled globally by api-gateway
# app.add_middleware(
#     CORSMiddleware,
#     allow_origins=["*"],
#     allow_credentials=True,
#     allow_methods=["*"],
#     allow_headers=["*"],
# )


@app.on_event("startup")
async def startup_event():
    await connect_to_mongo()
    await memory_service.connect()
    await kafka_service.connect()

@app.on_event("shutdown")
async def shutdown_event():
    await close_mongo_connection()
    await memory_service.disconnect()
    await kafka_service.disconnect()

# Include Routers
app.include_router(chat.router)

@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "ai-service"}
