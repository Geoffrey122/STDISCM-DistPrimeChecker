import subprocess
import time
import csv
import statistics
import os
import platform
import socket

def update_thread_count(file_name, thread_count):
    """Update the MAX_THREAD_COUNT in the specified Java file."""
    print(f"Updating thread count to {thread_count} in {file_name}...")
    with open(file_name, 'r') as file:
        lines = file.readlines()

    with open(file_name, 'w') as file:
        for line in lines:
            if "private static final int MAX_THREAD_COUNT" in line:
                file.write(f'    private static final int MAX_THREAD_COUNT = {thread_count}; // Number of threads\n')
            else:
                file.write(line)

def run_client(start, end):
    """Runs the client program and returns the runtime in milliseconds."""
    print(f"Running client for range {start} to {end}...")
    result = subprocess.run(['java', 'Client', str(start), str(end)], capture_output=True, text=True)
    runtime = None
    for line in result.stdout.split('\n'):
        if line.startswith('Runtime:'):
            runtime = int(line.split(':')[1].strip().split()[0])
    if runtime is None:
        raise RuntimeError("Failed to capture runtime from Client output.")
    return runtime

def send_batch_task_to_slave(slave_ip, tasks):
    """Sends a batch of tasks to the slave process and receives the results."""
    print(f"Sending batch tasks to slave at {slave_ip}...")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((slave_ip, 3101))
        task_str = ";".join([f"{start},{end}" for start, end in tasks])
        s.sendall(f"{task_str}\n".encode())
        result = s.recv(4096).decode()
    return result

def run_experiment(thread_counts, num_runs, slave_host, slave_ip, remote_java_path):
    results = []
    local_java_files = ['Master.java', 'PrimeChecker.java', 'Client.java']
    remote_java_file = 'Slave.java'
    
    # Terminate any existing slave processes
    print("Terminating any existing slave processes...")
    terminate_remote_process(slave_host, 'Slave')
    
    # Start Slave process on the remote machine once
    print("Starting slave process on remote machine...")
    ssh_start_command = f'ssh {slave_host} "nohup java -cp {remote_java_path} Slave > /dev/null 2>&1 &"'
    subprocess.run(ssh_start_command, shell=True)
    
    for thread_count in thread_counts:
        print(f'Running experiment with {thread_count} threads...')

        # Update thread count in all relevant Java files
        for java_file in local_java_files:
            update_thread_count(java_file, thread_count)
        # Update thread count in the remote Slave.java file
        print("Updating thread count in remote Slave.java...")
        ssh_update_command = f'ssh {slave_host} "sed -i .bak \'s/private static final int MAX_THREAD_COUNT = .*/private static final int MAX_THREAD_COUNT = {thread_count}; \/\/ Number of threads/\' {remote_java_path}/{remote_java_file}"'
        subprocess.run(ssh_update_command, shell=True)
        
        # Compile local Java files
        print("Compiling local Java files...")
        subprocess.run(['javac', *local_java_files])
        
        # Compile remote Slave.java file
        print("Compiling remote Slave.java file...")
        ssh_compile_command = f'ssh {slave_host} "cd {remote_java_path} && javac {remote_java_file}"'
        subprocess.run(ssh_compile_command, shell=True)

        # Start Master process locally
        print("Starting Master process locally...")
        master_process = subprocess.Popen(['java', 'Master'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        prime_checker_process = subprocess.Popen(['java', 'PrimeChecker'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # Let the servers start up
        print("Waiting for servers to start up...")
        time.sleep(5)

        # Define the batch of tasks
        print("Defining batch tasks...")
        num_tasks = 10
        task_size = 10000  # Adjust based on your requirements
        tasks = [(i * task_size, (i + 1) * task_size - 1) for i in range(num_tasks)]

        runtimes = []
        for run in range(num_runs):
            print(f"Starting run {run + 1} of {num_runs}...")
            for attempt in range(3):  # Run 3 times to allow OS to cache memory locations
                print(f"Run {run + 1}, attempt {attempt + 1}...")
                start_time = time.time()
                results_from_slave = send_batch_task_to_slave(slave_ip, tasks)
                end_time = time.time()
                runtime = (end_time - start_time) * 1000  # Convert to milliseconds
                runtimes.append(runtime)
                time.sleep(1)  # Give some time between runs

        # Calculate the mean runtime
        if runtimes:
            mean_runtime = statistics.mean(runtimes)
            results.append((thread_count, mean_runtime))
            print(f"Mean runtime for {thread_count} threads: {mean_runtime} ms")
        else:
            results.append((thread_count, None))

        # Terminate the servers
        print("Terminating Master and PrimeChecker processes...")
        terminate_process(master_process)
        terminate_process(prime_checker_process)

    # Terminate the remote Slave process
    print("Terminating remote Slave process...")
    terminate_remote_process(slave_host, 'Slave')

    return results

def terminate_process(process):
    if process.poll() is None:  # Check if the process is still running
        if platform.system() == "Windows":
            subprocess.run(['taskkill', '/F', '/T', '/PID', str(process.pid)])
        else:
            os.kill(process.pid, signal.SIGTERM)
        process.wait()  # Wait for the process to terminate
        print(f"Process {process.pid} terminated.")

def terminate_remote_process(slave_host, process_name):
    print(f"Terminating remote process {process_name} on {slave_host}...")
    ssh_command = f'ssh {slave_host} "pkill -f {process_name}"'
    subprocess.run(ssh_command, shell=True)

def save_to_csv(results, filename):
    print(f"Saving results to {filename}...")
    with open(filename, mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(['Thread Count', 'Mean Runtime (ms)'])
        writer.writerows(results)
    print(f"Results saved to {filename}.")

if __name__ == "__main__":
    thread_counts = [2**i for i in range(11)]  # 1, 2, 4, ..., 1024
    num_runs = 5
    slave_host = 'btbtest@192.168.1.10'  # Replace with your MacBook's SSH user and IP address
    slave_ip = '192.168.1.10'  # IP address or hostname of the slave machine
    remote_java_path = '/Volumes/Macintosh\\ HD\\ -\\ Data/ClientJava/ClientJava/src'  # Update with the actual path to your Java files on the MacBook

    results = run_experiment(thread_counts, num_runs, slave_host, slave_ip, remote_java_path)
    save_to_csv(results, 'results.csv')
    print('Experiment completed and results saved to results.csv')
