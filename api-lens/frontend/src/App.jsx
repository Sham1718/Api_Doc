import { useEffect, useState } from 'react'
import './App.css'
import { healthCheck } from './service/OpenApiService'

function App() {

  const[health,setHealth]=useState("");

  const HealthCheck=async()=>{
    try {
      const res =await healthCheck();
      console.log(res);
      setHealth(res.data.Status);
    } catch (error) {
      console.log(error);
    }
  }
  console.log(health)

  useEffect(()=>{
    HealthCheck();
  },[]
  )
  

  return (
    <>
  <h1 className=' bg-red-700 text-2xl font-bold'>shyam</h1>
    <div className='text-2xl text-orange-300 font-bold'>{health}</div>
    </>
 
  )
}

export default App
