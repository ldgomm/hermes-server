try {
  const status = rs.status();
  printjson({ replicaSet: 'already_initialized', status: status.ok });
} catch (error) {
  print('Initializing MongoDB replica set rs0...');
  rs.initiate({
    _id: 'rs0',
    members: [
      { _id: 0, host: 'localhost:27017' }
    ]
  });
  print('MongoDB replica set initialized.');
}
