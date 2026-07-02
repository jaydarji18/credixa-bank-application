import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { TrendingDown, ShieldCheck, Zap, Globe } from 'lucide-react';

const AuthLayout = ({ children, title, subtitle }) => {
  return (
    <div className="min-h-screen bg-app-bg flex items-center justify-center p-4 lg:p-8 selection:bg-primary-500/30">
      <div className="max-w-6xl w-full grid lg:grid-cols-2 bg-surface rounded-[32px] overflow-hidden shadow-2xl border border-border-subtle min-h-[700px]">
        
        {/* Left Side: Brand Panel - Always Dark Gradient */}
        <div className="hidden lg:flex flex-col justify-between p-12 bg-gradient-to-br from-primary-600 via-primary-700 to-secondary-800 relative overflow-hidden text-white">
          <div className="absolute top-0 right-0 w-96 h-96 bg-white opacity-5 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl" />
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary-900 opacity-40 rounded-full translate-y-1/2 -translate-x-1/2 blur-3xl" />

          <div className="relative z-10">
            <div className="flex items-center gap-3 mb-10">
              <div className="w-12 h-12 rounded-2xl bg-white flex items-center justify-center shadow-xl">
                <TrendingDown size={28} className="text-primary-600" />
              </div>
              <span className="text-2xl font-black tracking-tight">
                Credixa <span className="opacity-80">Pro</span>
              </span>
            </div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <h1 className="text-5xl font-black leading-tight mb-4">
                Banking, Reimagined.
              </h1>
              <p className="text-lg opacity-70 font-medium mb-12 max-w-sm leading-relaxed">
                Experience the future of personal finance with AI-powered insights and lightning-fast transactions.
              </p>
            </motion.div>

            <div className="space-y-6">
              {[
                { icon: ShieldCheck, text: "Enterprise-grade security encryption" },
                { icon: Zap, text: "Instant domestic and international transfers" },
                { icon: Globe, text: "Multi-currency support for global citizens" }
              ].map((feature, i) => (
                <motion.div 
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.4 + (i * 0.1) }}
                  key={i} 
                  className="flex items-center gap-4 opacity-90"
                >
                  <div className="w-10 h-10 rounded-xl bg-white opacity-10 flex items-center justify-center border border-white opacity-10">
                    <feature.icon size={20} />
                  </div>
                  <span className="font-semibold text-sm">{feature.text}</span>
                </motion.div>
              ))}
            </div>
          </div>

          <div className="relative z-10 pt-12">
            <p className="opacity-50 text-xs font-bold uppercase tracking-[0.2em]">
              (c) 2026 Credixa Bank Corp. All rights reserved.
            </p>
          </div>
        </div>

        {/* Right Side: Form Panel - Theme Aware */}
        <div className="p-8 lg:p-16 flex flex-col justify-center bg-surface relative overflow-hidden">
          <div className="lg:hidden flex items-center gap-3 mb-12">
            <div className="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center">
              <TrendingDown size={24} className="text-white" />
            </div>
            <span className="text-xl font-black text-app-text">Credixa Pro</span>
          </div>

          <AnimatePresence mode="wait">
            <motion.div
              key={window.location.pathname}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3, ease: "easeOut" }}
              className="w-full max-w-md mx-auto"
            >
              <header className="mb-10">
                <h2 className="text-3xl font-black text-app-text tracking-tight mb-2">
                  {title}
                </h2>
                <p className="text-muted-text font-medium">
                  {subtitle}
                </p>
              </header>

              {children}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
