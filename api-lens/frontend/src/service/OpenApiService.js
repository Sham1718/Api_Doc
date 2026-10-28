import axios from "axios";

const api =axios.create({baseURL:"http://localhost:8080"})

export const healthCheck=()=>{
    return api.get("/api/v1/health/");
}