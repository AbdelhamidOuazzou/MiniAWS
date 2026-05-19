from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel
from typing import Optional
import uvicorn
import logging
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
import json

# Configuration du logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="MiniAWS Semantic Cache Service",
    description="Microservice Python gérant ChromaDB pour le RAG de MiniAWS (Option B)",
    version="1.0.0"
)

# Modèles de données Pydantic
class CacheSearchRequest(BaseModel):
    prompt: str

class VmDeploymentRequest(BaseModel):
    nomServeur: str
    ramRecommandee: int
    cpuRecommande: int
    explication: str
    os: str

class CacheSaveRequest(BaseModel):
    prompt: str
    result: VmDeploymentRequest

# Initialisation de ChromaDB et Embeddings
try:
    # On utilise un modèle d'embedding léger open-source (tourne en local sans API key)
    embedding_function = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")
    vector_store = Chroma(
        collection_name="miniaws_semantic_cache", 
        embedding_function=embedding_function,
        persist_directory="./chroma_db"
    )
    logger.info("ChromaDB initialisé avec succès.")
except Exception as e:
    logger.error(f"Erreur lors de l'initialisation de ChromaDB : {e}")

@app.get("/")
def read_root():
    return {"status": "Semantic Cache Service is running", "vector_store": "ChromaDB"}

@app.post("/api/cache/search", response_model=VmDeploymentRequest)
def search_cache(request: CacheSearchRequest):
    logger.info(f"Recherche dans le cache pour : '{request.prompt}'")
    try:
        # On vérifie d'abord si la collection contient des documents
        # (Chroma throws an error if we search an empty collection)
        count = vector_store._collection.count()
        if count == 0:
            logger.info("Cache vide (0 documents)")
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found in cache")

        # Recherche des documents les plus similaires
        results = vector_store.similarity_search_with_score(request.prompt, k=1)
        
        if results:
            doc, score = results[0]
            # ChromaDB distance score: lower is better (0.0 is exact match)
            # A threshold of 0.9 allows for close semantic matches with HuggingFaceEmbeddings
            if score < 0.9:
                logger.info(f"Match trouvé ! Score: {score}")
                data = json.loads(doc.page_content)
                return VmDeploymentRequest(**data)
            else:
                logger.info(f"Aucun match assez similaire (Meilleur score: {score})")
                
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found in cache")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Erreur lors de la recherche: {e}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Error during search")

@app.post("/api/cache/save")
def save_to_cache(request: CacheSaveRequest):
    logger.info(f"Sauvegarde dans le cache : '{request.prompt}' -> {request.result.nomServeur}")
    try:
        # On sauvegarde le JSON du résultat comme contenu du document
        content = request.result.model_dump_json()
        
        vector_store.add_texts(
            texts=[content],
            metadatas=[{"prompt": request.prompt}]
        )
        return {"status": "success"}
    except Exception as e:
        logger.error(f"Erreur lors de la sauvegarde: {e}")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Failed to save")

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8001, reload=True)