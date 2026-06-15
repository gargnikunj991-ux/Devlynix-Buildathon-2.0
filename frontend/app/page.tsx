'use client';

import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen bg-[#6B6A54] text-[#F2F2F2] flex items-center justify-center">
      
      <div className="text-center space-y-6">
        
        <h1 className="text-5xl font-bold text-[#111111]">
          DEVLYNIX
        </h1>

        <p className="text-[#F2F2F2]/80 max-w-md mx-auto">
          Retro cyberpunk developer matchmaking platform.
        </p>

        <div className="flex gap-4 justify-center mt-6">
          
          <Link
            href="/login"
            className="bg-[#B03A3A] px-6 py-3 rounded-xl font-semibold hover:scale-105 transition"
          >
            Login
          </Link>

          <Link
            href="/register"
            className="border border-[#F2F2F2] px-6 py-3 rounded-xl hover:bg-[#111111]"
          >
            Register
          </Link>

        </div>

      </div>

    </main>
  );
}