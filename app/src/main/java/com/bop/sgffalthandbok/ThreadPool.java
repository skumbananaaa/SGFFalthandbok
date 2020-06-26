package com.bop.sgffalthandbok;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPool
{
    private static ThreadPoolExecutor s_ThreadPool = null;

    public static void Init()
    {
        if (s_ThreadPool == null || s_ThreadPool.isShutdown())
        {
            s_ThreadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        }
    }
    public static void Shutdown()
    {
        s_ThreadPool.shutdown();
    }

    public static void Execute(final Runnable runnable)
    {
        s_ThreadPool.execute(runnable);
    }
}
